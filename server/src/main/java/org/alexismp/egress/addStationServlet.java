/*
 * Copyright 2015 alexismp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alexismp.egress;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author alexismp
 */
public class addStationServlet extends HttpServlet {

    GeoFire geofire;
    Firebase firebase;
    Map<String, Object> emptyOwner = new HashMap<>();
    Map<String, Object> randomOwner = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        firebase = new Firebase("https://shining-inferno-9452.firebaseio.com");
        geofire = new GeoFire(firebase.child("_geofire"));
        login();
        emptyOwner.put("owner", "");
        randomOwner.put("owner", "foo@bar.com");
    }

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
        response.setContentType("text/html;charset=UTF-8");
        try (final PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet addStationServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Updating location for all stations... </h1>");

            resetData();

            out.println("<h1>... done.</h1>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // adds Geofire location for all 6441 stations
    private void resetData() {
        for (int i = 1; i <= 6441; i++) {
            final String key = "" + i;
            // use single event listener so no further callbacks are made
            // (and there is no need to remove the event listener)
            firebase.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    // Add an empty "owner" attribute to existing data
                    // randomly set this attribute to "foo@bar.com"
                    int random = (int) (Math.random() * 3);
                    if (random % 3 == 0) {
                        snapshot.getRef().updateChildren(emptyOwner);
                    } else {
                        snapshot.getRef().updateChildren(randomOwner);
                    }

                    // Adding Geofire data to firebase (under "[root]/_geofire" )
                    System.out.println("Adding station [" + key + "] to Geofire ... ");
                    GeoLocation location = new GeoLocation(
                            Float.parseFloat(snapshot.child("LATITUDE").getValue().toString()),
                            Float.parseFloat(snapshot.child("LONGITUDE").getValue().toString()));
                    geofire.setLocation(key, location);
                }

                @Override
                public void onCancelled(FirebaseError error) {
                }
            });

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

    private void login() {
        System.out.println("Loging in...");
        firebase.authWithPassword("alexis.mp@gmail.com", "foo",
                new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        System.out.println("Authenticated with password : " + authData.getProviderData().get("displayName"));
                        // Authentication just completed successfully :)
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError error) {
                        // Something went wrong :(
                    }
                });

        firebase.authWithOAuthToken("google", "<OAuth Token>", new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                System.out.println("Authenticated with Google: " + authData);
                // the Google user is now authenticated with Firebase
            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                // there was an error
            }
        });
    }

}
