package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class YggdrasilGameProfileRepository
implements GameProfileRepository {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BASE_URL = "https://api.mcnet.djelectro.me/";
    private static final String SEARCH_PAGE_URL = "https://api.mcnet.djelectro.me/profiles/";

    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;
    private final YggdrasilAuthenticationService authenticationService;

    public YggdrasilGameProfileRepository(YggdrasilAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public void findProfilesByNames(String[] names, Agent agent, ProfileLookupCallback callback) {
        final Set<String> criteria = Sets.newHashSet();
        for (final String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(name.toLowerCase());
            }
        }
        final int page = 0;
        for (final List<String> request : Iterables.partition(criteria, 2)) {
            int failCount = 0;
            boolean failed;
            do {
                failed = false;
                try {
                    final ProfileSearchResultsResponse response = this.authenticationService.makeRequest(HttpAuthenticationService.constantURL("https://api.mcnet.djelectro.me/profiles/" + agent.getName().toLowerCase()), request, ProfileSearchResultsResponse.class);
                    failCount = 0;
                    YggdrasilGameProfileRepository.LOGGER.debug("Page {} returned {} results, parsing", page,
                            response.getProfiles().length);
                    final Set<String> missing = Sets.newHashSet(request);
                    for (final GameProfile profile : response.getProfiles()) {
                        YggdrasilGameProfileRepository.LOGGER.debug("Successfully looked up profile {}", profile);
                        missing.remove(profile.getName().toLowerCase());
                        callback.onProfileLookupSucceeded(profile);
                    }
                    for (final String name2 : missing) {
                        YggdrasilGameProfileRepository.LOGGER.debug("Couldn't find profile {}", name2);
                        callback.onProfileLookupFailed(new GameProfile(null, name2), new ProfileNotFoundException(
                                "Server did not find the requested profile"));
                    }
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ex) {
                    }
                } catch (AuthenticationException e) {
                    if (++failCount == 3) {
                        for (final String name3 : request) {
                            YggdrasilGameProfileRepository.LOGGER.debug(
                                    "Couldn't find profile {} because of a server error", name3);
                            callback.onProfileLookupFailed(new GameProfile(null, name3), e);
                        }
                    } else {
                        try {
                            Thread.sleep(750L);
                        } catch (InterruptedException ex2) {
                        }
                        failed = true;
                    }
                }
            } while (failed);
        }
    }
}

