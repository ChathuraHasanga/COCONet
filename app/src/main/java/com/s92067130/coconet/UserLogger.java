package com.s92067130.coconet;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserLogger {

    public static void logUserAction(String action) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // User is not logged in, skip logging
            return;
        }
        String userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference logRef = FirebaseDatabase.getInstance(
                        "https://coconet-63d52-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("logs/users")
                .child(userUid)
                .push(); // Unique log ID

        UserLog log = new UserLog(action, System.currentTimeMillis());
        logRef.setValue(log);
    }
}
