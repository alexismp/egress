/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package devoxx.egress;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alexismp
 */
public class JavaResetter {

    Firebase firebase;
    Map<String, Object> emptyOwner = new HashMap<>();
    private static final String configFile = "config.properties";

    public static void main(String[] args) {
        String username = null;
        String password = null;
        if (args.length < 2) {
            System.out.println("Usage : java -jar JavaResetter.jar <asminusername> <adminpassword>");
            File file = new File(configFile);
            System.out.println("Getting username/password from file: " + file.getAbsolutePath());
            Properties p = new Properties();

            try (InputStream input = new FileInputStream(file)) {
                p.load(input);
                username = p.getProperty("username");
                password = p.getProperty("password");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            username = args[0];
            password = args[1];
        }
        new JavaResetter(username, password);
    }

    private JavaResetter(String username, String password) {
        firebase = new Firebase("https://shining-inferno-9452.firebaseio.com/stations");
        emptyOwner.put("owner", "");

        System.out.println("About to reset all stations.");
        try {
            resetData(username, password);
        } catch (InterruptedException ex) {
            Logger.getLogger(JavaResetter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void resetData(String username, String password) throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        firebase.authWithPassword(username, password, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                System.out.println("Auth succeeded: " + authData);
                countDownLatch.countDown();
            }

            @Override
            public void onAuthenticationError(FirebaseError error) {
                System.out.println("Auth Failed: " + error);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await(); // block waiting for auth outcome
        Map<String, Object> station = new HashMap<>();
        station.put("owner", "");
        station.put("when", 0);
        station.put("OwnerMail", "");

        for (int i = 1; i <= 6441; i++) {
            final String key = "" + i;
            // use single event listener so no further callbacks are made
            // (and there is no need to remove the event listener)
            firebase.child("" + i).updateChildren(station, new Firebase.CompletionListener() {
                @Override
                public void onComplete(FirebaseError error, Firebase ref) {
                    if (error != null) {
                        System.out.println("Data could not be saved. " + error.getMessage());
                    } else {
                        System.out.println("Data saved successfully.");
                    }
                }
            });

        }
    }

}
