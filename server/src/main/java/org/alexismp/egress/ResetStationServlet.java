/*
Copyright 2015 Google Inc. All rights reserved.
        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/
package org.alexismp.egress;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 *
 * @author alexismp
 */
public class ResetStationServlet extends HttpServlet {

    private final Logger log = Logger.getLogger(this.getClass().getName());
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String stationId = null;
        try {
            Firebase firebase = null;
            Enumeration params = request.getParameterNames();
            while (params.hasMoreElements()) {
                String param = (String) params.nextElement();
                stationId = request.getParameter(param);
                log.info("??? reset station servlet  ????????????? " + param + ":" + stationId);
            }

            final CountDownLatch countDownLatch = new CountDownLatch(1);
            firebase = new Firebase("https://shining-inferno-9452.firebaseio.com/stations");
            
            String username = null;
            String password = null;
            try (InputStream input = this.getServletContext().getResourceAsStream("/WEB-INF/client.properties")) {
                Properties p = new Properties();
                p.load(input);
                username = p.getProperty("username");
                password = p.getProperty("password");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            log.info("Got Firebase credentials for "+ username);

            firebase.authWithPassword(username, password, new Firebase.AuthResultHandler() {
                @Override
                public void onAuthenticated(AuthData authData) {
                    log.info("Auth succeeded: " + authData);
                    countDownLatch.countDown();
                }

                @Override
                public void onAuthenticationError(FirebaseError error) {
                    log.severe("Auth Failed: " + error);
                    countDownLatch.countDown();
                }
            });

            countDownLatch.await(); // block waiting for auth outcome
            if (firebase.getAuth() == null) {
                log.severe("Couldn't connect to Firebase. Cannot reset station.");
                return;
            }
            Map<String, Object> station = new HashMap<>();
            station.put("owner", "");
            station.put("when", 0);
            station.put("ownerMail", "");

            firebase.child(stationId).updateChildren(station, new Firebase.CompletionListener() {
                @Override
                public void onComplete(FirebaseError error, Firebase ref) {
                    if (error != null) {
                        log.severe("Could not reset Station. " + error.getMessage());
                    } else {
                        log.info("Station reset successfully.");
                    }
                }
            });
        } catch (InterruptedException ex) {
            log.severe(ex.toString());
        }

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
