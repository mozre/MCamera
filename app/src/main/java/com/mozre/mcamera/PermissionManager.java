package com.mozre.mcamera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


import java.util.ArrayList;
import java.util.List;

public class PermissionManager {


    public static void checkAndRequestPermission(Context context, String[] permissions) {
        List<String> list = new ArrayList<>();
        for (String permission : permissions) {
            if (!isGrantedPermission(context, permission)) {
                list.add(permission);
            }
        }
        if (!list.isEmpty()) {
            String[] perms = new String[list.size()];
            list.toArray(perms);
            requestPermission(context, perms);
        }
    }

    private static void requestPermission(Context context, String[] permissions) {
        ActivityCompat.requestPermissions((Activity) context, permissions, Constants.PERMISSION_REQUEST_CODE);
    }

    private static boolean isGrantedPermission(Context context, String permissionName) {
        return ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_GRANTED;
    }

}
