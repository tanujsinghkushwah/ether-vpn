package com.anonymous.ethervpn.utilities;

import android.net.Uri;

import com.anonymous.ethervpn.R;
import com.anonymous.ethervpn.model.Server;

import java.util.Comparator;

public class Utils {

    /**
     * Convert drawable image resource to string
     *
     * @param resourceId drawable image resource
     * @return image path
     */
    public static String getImgURL(int resourceId) {

        // Use BuildConfig.APPLICATION_ID instead of R.class.getPackage().getName() if both are not same
        return Uri.parse("android.resource://" + R.class.getPackage().getName() + "/" + resourceId).toString();
    }

    // Comparator for sorting based on the country name
    public static Comparator<Server> serverComparator = new Comparator<Server>() {
        @Override
        public int compare(Server server1, Server server2) {
            return server1.getCountry().compareToIgnoreCase(server2.getCountry());
        }
    };
}
