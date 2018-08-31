package com.android.settings.external;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class SignatureVerifier {
    private static final byte[] RELEASE_DIGEST_GMSCORE = new byte[]{-16, -3, 108, 91, 65, 15, 37, -53, 37, -61, -75, 51, 70, -56, -105, 47, -82, 48, -8, -18, 116, 17, -33, -111, 4, -128, -83, 107, 45, 96, -37, -125};
    private static final byte[] RELEASE_DIGEST_TIPS = new byte[]{14, 68, 121, -2, 25, 61, 1, -51, 70, 33, 95, -52, -48, -39, 35, 61, -20, 119, -2, -94, 89, -5, -52, -97, 9, 33, 25, -11, 10, -125, 114, -27};

    private static byte[] getDigestBytes(String packageName) {
        byte[] digestBytes;
        if (packageName.hashCode() == 40935373 && packageName.equals("com.google.android.apps.tips")) {
            digestBytes = RELEASE_DIGEST_TIPS;
        } else {
            digestBytes = RELEASE_DIGEST_GMSCORE;
        }
        return digestBytes;
    }

    private static boolean isCertWhitelisted(String packageName, byte[] signature) {
        byte[] digestBytes;
        try {
            digestBytes = MessageDigest.getInstance("SHA-256").digest(signature);
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new SecurityException("Failed to obtain SHA-256 digest impl.", noSuchAlgorithmException);
        }
        return Arrays.equals(digestBytes, SignatureVerifier.getDigestBytes(packageName));
    }

    public static boolean isPackageWhitelisted(Context context, String packageName) {
        PackageInfo info;
        String fullName;
        try {
            info = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            fullName = info.packageName;
        } catch (PackageManager.NameNotFoundException nameNotFoundException) {
            Log.e("SignatureVerifier", "Could not find package name.", nameNotFoundException);
            return false;
        }

        if (!SignatureVerifier.verifyWhitelistedPackage(fullName))
            return false;

        return isSignatureWhitelisted(info);
    }

    private static boolean isSignatureWhitelisted(PackageInfo packageInfo) {
        // Make sure package only has 1 signature
        if (packageInfo.signatures.length != 1)
            return false;

        byte[] signature = packageInfo.signatures[0].toByteArray();
        return SignatureVerifier.isCertWhitelisted(packageInfo.packageName, signature);
    }

    private static String isUidWhitelisted(Context context, int uid) {
        String[] packages = context.getPackageManager().getPackagesForUid(uid);

        if (ArrayUtils.isEmpty(packages))
            return null;

        for (String packageName : packages) {
            if (!SignatureVerifier.isPackageWhitelisted(context, packageName))
                continue;

            return packageName;
        }
        return null;
    }

    public static String verifyCallerIsWhitelisted(Context context, int uid) throws SecurityException {
        String whitelist = SignatureVerifier.isUidWhitelisted(context, uid);
        if (!TextUtils.isEmpty(whitelist))
            return whitelist;
        throw new SecurityException("UID is not Google Signed");
    }

    private static boolean verifyWhitelistedPackage(String packageName) {
        boolean whitelisted = "com.google.android.googlequicksearchbox".equals(packageName)
                            || "com.google.android.gms".equals(packageName)
                            || "com.google.android.apps.tips".equals(packageName);
        return whitelisted;
    }
}

