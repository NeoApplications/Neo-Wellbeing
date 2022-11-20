package org.eu.droid_ng.wellbeing.framework.shim;

import android.os.Process;
import android.os.UserHandle;

@SuppressWarnings("JavaReflectionMemberAccess")
public class UserHandlerShim {
    public static final UserHandle ALL;

    static {
        UserHandle UserHandler_ALL;
        try {
            UserHandler_ALL = (UserHandle) UserHandle.class
                    .getDeclaredField("ALL").get(null);
        } catch (Exception ignored) {
            UserHandler_ALL = UserHandle.getUserHandleForUid(Process.myUid());
        }
        ALL = UserHandler_ALL;
    }
}
