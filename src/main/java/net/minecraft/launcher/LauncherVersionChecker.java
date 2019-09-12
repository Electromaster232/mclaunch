package net.minecraft.launcher;

import com.mojang.launcher.Http;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.net.Proxy;
import java.net.URL;

public class LauncherVersionChecker {

    public void runCheck(Proxy proxy){
        try {
            String oof = Http.performGet(new URL("https://launchermeta.mcnet.djelectro.me/vcheck"), proxy);
            if (!oof.equals(LauncherConstants.getVersionName())){
                JFrame parent = new JFrame();
                JOptionPane.showMessageDialog(parent, "There is an update available! Please connect to SMB to get it.");
            }
        }catch (Exception e){
            e.printStackTrace();
        }



    }
}
