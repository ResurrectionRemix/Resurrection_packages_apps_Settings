/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.content.Context;
import android.content.res.Resources;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.UserManager;
import android.provider.Settings;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import com.android.settings.ProxySelector;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.wifi.AccessPoint;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * The class for allowing UIs like {@link WifiDialog} and {@link WifiConfigUiBase} to
 * share the logic for controlling buttons, text fields, etc.
 */
public class WifiConfigController implements TextWatcher,
        AdapterView.OnItemSelectedListener, OnCheckedChangeListener,
        TextView.OnEditorActionListener, View.OnKeyListener{
    private static final String TAG = "WifiConfigController";

    private static final String SYSTEM_CA_STORE_PATH = "/system/etc/security/cacerts";

    private final WifiConfigUiBase mConfigUi;
    private final View mView;
    private final AccessPoint mAccessPoint;

    /* This value comes from "wifi_ip_settings" resource array */
    private static final int DHCP = 0;
    private static final int STATIC_IP = 1;

    /* These values come from "wifi_proxy_settings" resource array */
    public static final int PROXY_NONE = 0;
    public static final int PROXY_STATIC = 1;
    public static final int PROXY_PAC = 2;

    /* These values come from "wifi_eap_method" resource array */
    public static final int WIFI_EAP_METHOD_PEAP = 0;
    public static final int WIFI_EAP_METHOD_TLS  = 1;
    public static final int WIFI_EAP_METHOD_TTLS = 2;
    public static final int WIFI_EAP_METHOD_PWD  = 3;
    public static final int WIFI_EAP_METHOD_SIM  = 4;
    public static final int WIFI_EAP_METHOD_AKA  = 5;
    public static final int WIFI_EAP_METHOD_AKA_PRIME  = 6;

    /* These values come from "wifi_peap_phase2_entries" resource array */
    public static final int WIFI_PEAP_PHASE2_NONE       = 0;
    public static final int WIFI_PEAP_PHASE2_MSCHAPV2   = 1;
    public static final int WIFI_PEAP_PHASE2_GTC        = 2;

    /* Phase2 methods supported by PEAP are limited */
    private final ArrayAdapter<String> mPhase2PeapAdapter;
    /* Full list of phase2 methods */
    private final ArrayAdapter<String> mPhase2FullAdapter;

    private final Handler mTextViewChangedHandler;

    // e.g. AccessPoint.SECURITY_NONE
    private int mAccessPointSecurity;
    private TextView mPasswordView;

    private String mUnspecifiedCertString;
    private String mMultipleCertSetString;
    private String mUseSystemCertsString;
    private String mDoNotProvideEapUserCertString;
    private String mDoNotValidateEapServerString;

    private Spinner mSecuritySpinner;
    private Spinner mEapMethodSpinner;
    private Spinner mEapCaCertSpinner;
    private TextView mEapDomainView;
    private Spinner mPhase2Spinner;
    // Associated with mPhase2Spinner, one of mPhase2FullAdapter or mPhase2PeapAdapter
    private ArrayAdapter<String> mPhase2Adapter;
    private Spinner mEapUserCertSpinner;
    private TextView mEapIdentityView;
    private TextView mEapAnonymousView;

    private Spinner mSimCardSpinner;
    private ArrayList<String> mSimDisplayNames;

    private Spinner mIpSettingsSpinner;
    private TextView mIpAddressView;
    private TextView mGatewayView;
    private TextView mNetworkPrefixLengthView;
    private TextView mDns1View;
    private TextView mDns2View;

    private Spinner mProxySettingsSpinner;
    private TextView mProxyHostView;
    private TextView mProxyPortView;
    private TextView mProxyExclusionListView;
    private TextView mProxyPacView;

    private CheckBox mSharedCheckBox;

    private IpAssignment mIpAssignment = IpAssignment.UNASSIGNED;
    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;

    private String[] mLevels;
    private int mMode;
    private TextView mSsidView;

    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager = null;
    private int selectedSimCardNumber;

    public WifiConfigController(WifiConfigUiBase parent, View view, AccessPoint accessPoint,
            int mode) {
        mConfigUi = parent;

        mView = view;
        mAccessPoint = accessPoint;
        mAccessPointSecurity = (accessPoint == null) ? AccessPoint.SECURITY_NONE :
                accessPoint.getSecurity();
        mMode = mode;

        mTextViewChangedHandler = new Handler();
        mContext = mConfigUi.getContext();
        final Resources res = mContext.getResources();

        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mSimDisplayNames = new ArrayList<String>();
        mLevels = res.getStringArray(R.array.wifi_signal);
        mPhase2PeapAdapter = new ArrayAdapter<String>(
            mContext, android.R.layout.simple_spinner_item,
            res.getStringArray(R.array.wifi_peap_phase2_entries));
        mPhase2PeapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mPhase2FullAdapter = new ArrayAdapter<String>(
                mContext, android.R.layout.simple_spinner_item,
                res.getStringArray(R.array.wifi_phase2_entries));
        mPhase2FullAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mUnspecifiedCertString = mContext.getString(R.string.wifi_unspecified);
        mMultipleCertSetString = mContext.getString(R.string.wifi_multiple_cert_added);
        mUseSystemCertsString = mContext.getString(R.string.wifi_use_system_certs);
        mDoNotProvideEapUserCertString =
            mContext.getString(R.string.wifi_do_not_provide_eap_user_cert);
        mDoNotValidateEapServerString =
            mContext.getString(R.string.wifi_do_not_validate_eap_server);

        mIpSettingsSpinner = (Spinner) mView.findViewById(R.id.ip_settings);
        mIpSettingsSpinner.setOnItemSelectedListener(this);
        mProxySettingsSpinner = (Spinner) mView.findViewById(R.id.proxy_settings);
        mProxySettingsSpinner.setOnItemSelectedListener(this);
        mSharedCheckBox = (CheckBox) mView.findViewById(R.id.shared);

        if (mAccessPoint == null) { // new network
            mConfigUi.setTitle(R.string.wifi_add_network);

            mSsidView = (TextView) mView.findViewById(R.id.ssid);
            mSsidView.addTextChangedListener(this);
            mSecuritySpinner = ((Spinner) mView.findViewById(R.id.security));
            mSecuritySpinner.setOnItemSelectedListener(this);
            mView.findViewById(R.id.type).setVisibility(View.VISIBLE);

            showIpConfigFields();
            showProxyFields();
            mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
            ((CheckBox) mView.findViewById(R.id.wifi_advanced_togglebox))
                    .setOnCheckedChangeListener(this);

            mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
        } else {
            mConfigUi.setTitle(mAccessPoint.getSsid());

            ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);

            boolean showAdvancedFields = false;
            if (mAccessPoint.isSaved()) {
                WifiConfiguration config = mAccessPoint.getConfig();
                if (config.getIpAssignment() == IpAssignment.STATIC) {
                    mIpSettingsSpinner.setSelection(STATIC_IP);
                    showAdvancedFields = true;
                    // Display IP address.
                    StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                    if (staticConfig != null && staticConfig.ipAddress != null) {
                        addRow(group, R.string.wifi_ip_address,
                                staticConfig.ipAddress.getAddress().getHostAddress());
                    }
                } else {
                    mIpSettingsSpinner.setSelection(DHCP);
                }

                mSharedCheckBox.setEnabled(config.shared);
                if (!config.shared) {
                    showAdvancedFields = true;
                }

                if (config.getProxySettings() == ProxySettings.STATIC) {
                    mProxySettingsSpinner.setSelection(PROXY_STATIC);
                    showAdvancedFields = true;
                } else if (config.getProxySettings() == ProxySettings.PAC) {
                    mProxySettingsSpinner.setSelection(PROXY_PAC);
                    showAdvancedFields = true;
                } else {
                    mProxySettingsSpinner.setSelection(PROXY_NONE);
                }
                if (config != null && config.isPasspoint()) {
                    addRow(group, R.string.passpoint_label,
                            String.format(mContext.getString(R.string.passpoint_content),
                            config.providerFriendlyName));
                }
            }

            if ((!mAccessPoint.isSaved() && !mAccessPoint.isActive())
                    || mMode != WifiConfigUiBase.MODE_VIEW) {
                showSecurityFields();
                showIpConfigFields();
                showProxyFields();
                final CheckBox advancedTogglebox =
                        (CheckBox) mView.findViewById(R.id.wifi_advanced_togglebox);
                mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
                advancedTogglebox.setOnCheckedChangeListener(this);
                advancedTogglebox.setChecked(showAdvancedFields);
                mView.findViewById(R.id.wifi_advanced_fields)
                        .setVisibility(showAdvancedFields ? View.VISIBLE : View.GONE);
            }

            if (mMode == WifiConfigUiBase.MODE_MODIFY) {
                mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            } else if (mMode == WifiConfigUiBase.MODE_CONNECT) {
                mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
            } else {
                final DetailedState state = mAccessPoint.getDetailedState();
                final String signalLevel = getSignalString();

                if (state == null && signalLevel != null) {
                    mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
                } else {
                    if (state != null) {
                        boolean isEphemeral = mAccessPoint.isEphemeral();
                        WifiConfiguration config = mAccessPoint.getConfig();
                        String providerFriendlyName = null;
                        if (config != null && config.isPasspoint()) {
                            providerFriendlyName = config.providerFriendlyName;
                        }
                        String summary = AccessPoint.getSummary(
                                mConfigUi.getContext(), state, isEphemeral, providerFriendlyName);
                        addRow(group, R.string.wifi_status, summary);
                    }

                    if (signalLevel != null) {
                        addRow(group, R.string.wifi_signal, signalLevel);
                    }

                    WifiInfo info = mAccessPoint.getInfo();
                    if (info != null && info.getLinkSpeed() != -1) {
                        addRow(group, R.string.wifi_speed, String.format(
                                res.getString(R.string.link_speed), info.getLinkSpeed()));
                    }

                    if (info != null && info.getFrequency() != -1) {
                        final int frequency = info.getFrequency();
                        String band = null;

                        if (frequency >= AccessPoint.LOWER_FREQ_24GHZ
                                && frequency < AccessPoint.HIGHER_FREQ_24GHZ) {
                            band = res.getString(R.string.wifi_band_24ghz);
                        } else if (frequency >= AccessPoint.LOWER_FREQ_5GHZ
                                && frequency < AccessPoint.HIGHER_FREQ_5GHZ) {
                            band = res.getString(R.string.wifi_band_5ghz);
                        } else {
                            Log.e(TAG, "Unexpected frequency " + frequency);
                        }
                        if (band != null) {
                            addRow(group, R.string.wifi_frequency, band);
                        }
                    }

                    addRow(group, R.string.wifi_security, mAccessPoint.getSecurityString(false));
                    mView.findViewById(R.id.ip_fields).setVisibility(View.GONE);
                }
                if (mAccessPoint.isSaved() || mAccessPoint.isActive()) {
                    mConfigUi.setForgetButton(res.getString(R.string.wifi_forget));
                }
            }
        }

        final UserManager userManager =
                (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        if (!userManager.isSplitSystemUser()) {
            mSharedCheckBox.setVisibility(View.GONE);
        }

        mConfigUi.setCancelButton(res.getString(R.string.wifi_cancel));
        if (mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    private String getSignalString() {
        final int level = mAccessPoint.getLevel();

        return (level > -1 && level < mLevels.length) ? mLevels[level] : null;
    }

    void hideForgetButton() {
        Button forget = mConfigUi.getForgetButton();
        if (forget == null) return;

        forget.setVisibility(View.GONE);
    }

    void hideSubmitButton() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        submit.setVisibility(View.GONE);
    }

    /* show submit button if password, ip and proxy settings are valid */
    void enableSubmitIfAppropriate() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        submit.setEnabled(isSubmittable());
    }

    boolean isSubmittable() {
        boolean enabled = false;
        boolean passwordInvalid = false;

        if (mPasswordView != null
                && ((mAccessPointSecurity == AccessPoint.SECURITY_WEP
                        && mPasswordView.length() == 0)
                    || (mAccessPointSecurity == AccessPoint.SECURITY_PSK
                           && mPasswordView.length() < 8))) {
            passwordInvalid = true;
        }

        if ((mSsidView != null && mSsidView.length() == 0)
                || ((mAccessPoint == null || !mAccessPoint.isSaved()) && passwordInvalid)) {
            enabled = false;
        } else {
            enabled = ipAndProxyFieldsAreValid();
        }
        if (mEapCaCertSpinner != null
                && mView.findViewById(R.id.l_ca_cert).getVisibility() != View.GONE) {
            String caCertSelection = (String) mEapCaCertSpinner.getSelectedItem();
            if (caCertSelection.equals(mUnspecifiedCertString)) {
                // Disallow submit if the user has not selected a CA certificate for an EAP network
                // configuration.
                enabled = false;
            }
            if (caCertSelection.equals(mUseSystemCertsString)
                    && mEapDomainView != null
                    && mView.findViewById(R.id.l_domain).getVisibility() != View.GONE
                    && TextUtils.isEmpty(mEapDomainView.getText().toString())) {
                // Disallow submit if the user chooses to use system certificates for EAP server
                // validation, but does not provide a domain.
                enabled = false;
            }
        }
        if (mEapUserCertSpinner != null
                && mView.findViewById(R.id.l_user_cert).getVisibility() != View.GONE
                && ((String) mEapUserCertSpinner.getSelectedItem())
                       .equals(mUnspecifiedCertString)) {
            // Disallow submit if the user has not selected a user certificate for an EAP network
            // configuration.
            enabled = false;
        }
        return enabled;
    }

    void showWarningMessagesIfAppropriate() {
        mView.findViewById(R.id.no_ca_cert_warning).setVisibility(View.GONE);
        mView.findViewById(R.id.no_domain_warning).setVisibility(View.GONE);

        if (mEapCaCertSpinner != null
                && mView.findViewById(R.id.l_ca_cert).getVisibility() != View.GONE) {
            String caCertSelection = (String) mEapCaCertSpinner.getSelectedItem();
            if (caCertSelection.equals(mDoNotValidateEapServerString)) {
                // Display warning if user chooses not to validate the EAP server with a
                // user-supplied CA certificate in an EAP network configuration.
                mView.findViewById(R.id.no_ca_cert_warning).setVisibility(View.VISIBLE);
            }
            if (caCertSelection.equals(mUseSystemCertsString)
                    && mEapDomainView != null
                    && mView.findViewById(R.id.l_domain).getVisibility() != View.GONE
                    && TextUtils.isEmpty(mEapDomainView.getText().toString())) {
                // Display warning if user chooses to use pre-installed public CA certificates
                // without restricting the server domain that these certificates can be used to
                // validate.
                mView.findViewById(R.id.no_domain_warning).setVisibility(View.VISIBLE);
            }
        }
    }

    /* package */ WifiConfiguration getConfig() {
        if (mMode == WifiConfigUiBase.MODE_VIEW) {
            return null;
        }

        WifiConfiguration config = new WifiConfiguration();

        if (mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mSsidView.getText().toString());
            // If the user adds a network manually, assume that it is hidden.
            config.hiddenSSID = true;
        } else if (!mAccessPoint.isSaved()) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mAccessPoint.getSsidStr());
        } else {
            config.networkId = mAccessPoint.getConfig().networkId;
        }

        config.shared = mSharedCheckBox.isChecked();

        switch (mAccessPointSecurity) {
            case AccessPoint.SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;

            case AccessPoint.SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if (mPasswordView.length() != 0) {
                    int length = mPasswordView.length();
                    String password = mPasswordView.getText().toString();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58)
                            && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                if (mPasswordView.length() != 0) {
                    String password = mPasswordView.getText().toString();
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_EAP:
                config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = mEapMethodSpinner.getSelectedItemPosition();
                int phase2Method = mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        // PEAP supports limited phase2 values
                        // Map the index from the mPhase2PeapAdapter to the one used
                        // by the API which has the full list of PEAP methods.
                        switch(phase2Method) {
                            case WIFI_PEAP_PHASE2_NONE:
                                config.enterpriseConfig.setPhase2Method(Phase2.NONE);
                                break;
                            case WIFI_PEAP_PHASE2_MSCHAPV2:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAPV2);
                                break;
                            case WIFI_PEAP_PHASE2_GTC:
                                config.enterpriseConfig.setPhase2Method(Phase2.GTC);
                                break;
                            default:
                                Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    case Eap.SIM:
                    case Eap.AKA:
                    case Eap.AKA_PRIME:
                        selectedSimCardNumber = mSimCardSpinner.getSelectedItemPosition() + 1;
                        config.SIMNum = selectedSimCardNumber;
                        break;
                    default:
                        // The default index from mPhase2FullAdapter maps to the API
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }

                String caCert = (String) mEapCaCertSpinner.getSelectedItem();
                config.enterpriseConfig.setCaCertificateAliases(null);
                config.enterpriseConfig.setCaPath(null);
                config.enterpriseConfig.setDomainSuffixMatch(mEapDomainView.getText().toString());
                if (caCert.equals(mUnspecifiedCertString)
                        || caCert.equals(mDoNotValidateEapServerString)) {
                    // ca_cert already set to null, so do nothing.
                } else if (caCert.equals(mUseSystemCertsString)) {
                    config.enterpriseConfig.setCaPath(SYSTEM_CA_STORE_PATH);
                } else if (caCert.equals(mMultipleCertSetString)) {
                    if (mAccessPoint != null) {
                        if (!mAccessPoint.isSaved()) {
                            Log.e(TAG, "Multiple certs can only be set "
                                    + "when editing saved network");
                        }
                        config.enterpriseConfig.setCaCertificateAliases(
                                mAccessPoint
                                        .getConfig()
                                        .enterpriseConfig
                                        .getCaCertificateAliases());
                    }
                } else {
                    config.enterpriseConfig.setCaCertificateAliases(new String[] {caCert});
                }

                // ca_cert or ca_path should not both be non-null, since we only intend to let
                // the use either their own certificate, or the system certificates, not both.
                // The variable that is not used must explicitly be set to null, so that a
                // previously-set value on a saved configuration will be erased on an update.
                if (config.enterpriseConfig.getCaCertificateAliases() != null
                        && config.enterpriseConfig.getCaPath() != null) {
                    Log.e(TAG, "ca_cert ("
                            + config.enterpriseConfig.getCaCertificateAliases()
                            + ") and ca_path ("
                            + config.enterpriseConfig.getCaPath()
                            + ") should not both be non-null");
                }

                String clientCert = (String) mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(mUnspecifiedCertString)
                        || clientCert.equals(mDoNotProvideEapUserCertString)) {
                    // Note: |clientCert| should not be able to take the value |unspecifiedCert|,
                    // since we prevent such configurations from being saved.
                    clientCert = "";
                }
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                if (eapMethod == Eap.SIM || eapMethod == Eap.AKA || eapMethod == Eap.AKA_PRIME) {
                    config.enterpriseConfig.setIdentity("");
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else if (eapMethod == Eap.PWD) {
                    config.enterpriseConfig.setIdentity(mEapIdentityView.getText().toString());
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else {
                    config.enterpriseConfig.setIdentity(mEapIdentityView.getText().toString());
                    config.enterpriseConfig.setAnonymousIdentity(
                            mEapAnonymousView.getText().toString());
                }

                if (mPasswordView.isShown()) {
                    // For security reasons, a previous password is not displayed to user.
                    // Update only if it has been changed.
                    if (mPasswordView.length() > 0) {
                        config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                    }
                } else {
                    // clear password
                    config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                }
                break;
            default:
                return null;
        }

        config.setIpConfiguration(
                new IpConfiguration(mIpAssignment, mProxySettings,
                                    mStaticIpConfiguration, mHttpProxy));

        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        mIpAssignment =
                (mIpSettingsSpinner != null
                    && mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP)
                ? IpAssignment.STATIC
                : IpAssignment.DHCP;

        if (mIpAssignment == IpAssignment.STATIC) {
            mStaticIpConfiguration = new StaticIpConfiguration();
            int result = validateIpConfigFields(mStaticIpConfiguration);
            if (result != 0) {
                return false;
            }
        }

        final int selectedPosition = mProxySettingsSpinner.getSelectedItemPosition();
        mProxySettings = ProxySettings.NONE;
        mHttpProxy = null;
        if (selectedPosition == PROXY_STATIC && mProxyHostView != null) {
            mProxySettings = ProxySettings.STATIC;
            String host = mProxyHostView.getText().toString();
            String portStr = mProxyPortView.getText().toString();
            String exclusionList = mProxyExclusionListView.getText().toString();
            int port = 0;
            int result = 0;
            try {
                port = Integer.parseInt(portStr);
                result = ProxySelector.validate(host, portStr, exclusionList);
            } catch (NumberFormatException e) {
                result = R.string.proxy_error_invalid_port;
            }
            if (result == 0) {
                mHttpProxy = new ProxyInfo(host, port, exclusionList);
            } else {
                return false;
            }
        } else if (selectedPosition == PROXY_PAC && mProxyPacView != null) {
            mProxySettings = ProxySettings.PAC;
            CharSequence uriSequence = mProxyPacView.getText();
            if (TextUtils.isEmpty(uriSequence)) {
                return false;
            }
            Uri uri = Uri.parse(uriSequence.toString());
            if (uri == null) {
                return false;
            }
            mHttpProxy = new ProxyInfo(uri);
        }
        return true;
    }

    private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        if (mIpAddressView == null) return 0;

        String ipAddr = mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr)) return R.string.wifi_ip_settings_invalid_ip_address;

        Inet4Address inetAddr = getIPv4Address(ipAddr);
        if (inetAddr == null || inetAddr.equals(Inet4Address.ANY)) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }

        int networkPrefixLength = -1;
        try {
            networkPrefixLength = Integer.parseInt(mNetworkPrefixLengthView.getText().toString());
            if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                return R.string.wifi_ip_settings_invalid_network_prefix_length;
            }
            staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
        } catch (NumberFormatException e) {
            // Set the hint as default after user types in ip address
            mNetworkPrefixLengthView.setText(mConfigUi.getContext().getString(
                    R.string.wifi_network_prefix_length_hint));
        } catch (IllegalArgumentException e) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }

        String gateway = mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            try {
                //Extract a default gateway from IP address
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length - 1] = 1;
                mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
            } catch (RuntimeException ee) {
            } catch (java.net.UnknownHostException u) {
            }
        } else {
            InetAddress gatewayAddr = getIPv4Address(gateway);
            if (gatewayAddr == null) {
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            if (gatewayAddr.isMulticastAddress()) {
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            staticIpConfiguration.gateway = gatewayAddr;
        }

        String dns = mDns1View.getText().toString();
        InetAddress dnsAddr = null;

        if (TextUtils.isEmpty(dns)) {
            //If everything else is valid, provide hint as a default option
            mDns1View.setText(mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
        } else {
            dnsAddr = getIPv4Address(dns);
            if (dnsAddr == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(dnsAddr);
        }

        if (mDns2View.length() > 0) {
            dns = mDns2View.getText().toString();
            dnsAddr = getIPv4Address(dns);
            if (dnsAddr == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(dnsAddr);
        }
        return 0;
    }

    private void showSecurityFields() {
        if (mAccessPointSecurity == AccessPoint.SECURITY_NONE) {
            mView.findViewById(R.id.security_fields).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.security_fields).setVisibility(View.VISIBLE);

        if (mPasswordView == null) {
            mPasswordView = (TextView) mView.findViewById(R.id.password);
            mPasswordView.addTextChangedListener(this);
            mPasswordView.setOnEditorActionListener(this);
            mPasswordView.setOnKeyListener(this);
            ((CheckBox) mView.findViewById(R.id.show_password))
                .setOnCheckedChangeListener(this);

            if (mAccessPoint != null && mAccessPoint.isSaved()) {
                mPasswordView.setHint(R.string.wifi_unchanged);
            }
        }

        if (mAccessPointSecurity != AccessPoint.SECURITY_EAP) {
            mView.findViewById(R.id.eap).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.eap).setVisibility(View.VISIBLE);

        if (mEapMethodSpinner == null) {
            getSIMInfo();
            mEapMethodSpinner = (Spinner) mView.findViewById(R.id.method);
            mEapMethodSpinner.setOnItemSelectedListener(this);
            if (Utils.isWifiOnly(mContext) || !mContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_eap_sim_based_auth_supported)) {
                String[] eapMethods = mContext.getResources().getStringArray(
                        R.array.eap_method_without_sim_auth);
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mContext,
                        android.R.layout.simple_spinner_item, eapMethods);
                spinnerAdapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item);
                mEapMethodSpinner.setAdapter(spinnerAdapter);
            }
            mPhase2Spinner = (Spinner) mView.findViewById(R.id.phase2);
            mEapCaCertSpinner = (Spinner) mView.findViewById(R.id.ca_cert);
            mEapCaCertSpinner.setOnItemSelectedListener(this);
            mEapDomainView = (TextView) mView.findViewById(R.id.domain);
            mEapDomainView.addTextChangedListener(this);
            mEapUserCertSpinner = (Spinner) mView.findViewById(R.id.user_cert);
            mEapUserCertSpinner.setOnItemSelectedListener(this);
            mSimCardSpinner = (Spinner) mView.findViewById(R.id.sim_card);
            mEapIdentityView = (TextView) mView.findViewById(R.id.identity);
            mEapAnonymousView = (TextView) mView.findViewById(R.id.anonymous);

            loadCertificates(
                    mEapCaCertSpinner,
                    Credentials.CA_CERTIFICATE,
                    mDoNotValidateEapServerString,
                    false,
                    true);
            loadCertificates(
                    mEapUserCertSpinner,
                    Credentials.USER_PRIVATE_KEY,
                    mDoNotProvideEapUserCertString,
                    false,
                    false);

            // Modifying an existing network
            if (mAccessPoint != null && mAccessPoint.isSaved()) {
                WifiEnterpriseConfig enterpriseConfig = mAccessPoint.getConfig().enterpriseConfig;
                int eapMethod = enterpriseConfig.getEapMethod();
                int phase2Method = enterpriseConfig.getPhase2Method();
                mEapMethodSpinner.setSelection(eapMethod);
                showEapFieldsByMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        switch (phase2Method) {
                            case Phase2.NONE:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_NONE);
                                break;
                            case Phase2.MSCHAPV2:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_MSCHAPV2);
                                break;
                            case Phase2.GTC:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_GTC);
                                break;
                            default:
                                Log.e(TAG, "Invalid phase 2 method " + phase2Method);
                                break;
                        }
                        break;
                    case Eap.SIM:
                    case Eap.AKA:
                    case Eap.AKA_PRIME:
                        WifiConfiguration config =  mAccessPoint.getConfig();
                        mSimCardSpinner.setSelection(config.SIMNum-1);
                        break;
                    default:
                        mPhase2Spinner.setSelection(phase2Method);
                        break;
                }
                if (!TextUtils.isEmpty(enterpriseConfig.getCaPath())) {
                    setSelection(mEapCaCertSpinner, mUseSystemCertsString);
                } else {
                    String[] caCerts = enterpriseConfig.getCaCertificateAliases();
                    if (caCerts == null) {
                        setSelection(mEapCaCertSpinner, mDoNotValidateEapServerString);
                    } else if (caCerts.length == 1) {
                        setSelection(mEapCaCertSpinner, caCerts[0]);
                    } else {
                        // Reload the cert spinner with an extra "multiple certificates added" item.
                        loadCertificates(
                                mEapCaCertSpinner,
                                Credentials.CA_CERTIFICATE,
                                mDoNotValidateEapServerString,
                                true,
                                true);
                        setSelection(mEapCaCertSpinner, mMultipleCertSetString);
                    }
                }
                mEapDomainView.setText(enterpriseConfig.getDomainSuffixMatch());
                String userCert = enterpriseConfig.getClientCertificateAlias();
                if (TextUtils.isEmpty(userCert)) {
                    setSelection(mEapUserCertSpinner, mDoNotProvideEapUserCertString);
                } else {
                    setSelection(mEapUserCertSpinner, userCert);
                }
                mEapIdentityView.setText(enterpriseConfig.getIdentity());
                mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
            } else {
                showEapFieldsByMethod(mEapMethodSpinner.getSelectedItemPosition());
            }
        } else {
            showEapFieldsByMethod(mEapMethodSpinner.getSelectedItemPosition());
        }
    }

    /**
     * EAP-PWD valid fields include
     *   identity
     *   password
     * EAP-PEAP valid fields include
     *   phase2: MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     * EAP-TLS valid fields include
     *   user_cert
     *   ca_cert
     *   domain
     *   identity
     * EAP-TTLS valid fields include
     *   phase2: PAP, MSCHAP, MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     */
    private void showEapFieldsByMethod(int eapMethod) {
        // Common defaults
        mView.findViewById(R.id.l_method).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_identity).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_domain).setVisibility(View.VISIBLE);

        // Defaults for most of the EAP methods and over-riden by
        // by certain EAP methods
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.password_layout).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.VISIBLE);

        Context context = mConfigUi.getContext();
        switch (eapMethod) {
            case WIFI_EAP_METHOD_PWD:
                setPhase2Invisible();
                setCaCertInvisible();
                setDomainInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                setSimCardInvisible();
                break;
            case WIFI_EAP_METHOD_TLS:
                mView.findViewById(R.id.l_user_cert).setVisibility(View.VISIBLE);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                setSimCardInvisible();
                break;
            case WIFI_EAP_METHOD_PEAP:
                // Reset adapter if needed
                if (mPhase2Adapter != mPhase2PeapAdapter) {
                    mPhase2Adapter = mPhase2PeapAdapter;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                setUserCertInvisible();
                setSimCardInvisible();
                break;
            case WIFI_EAP_METHOD_TTLS:
                // Reset adapter if needed
                if (mPhase2Adapter != mPhase2FullAdapter) {
                    mPhase2Adapter = mPhase2FullAdapter;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                setUserCertInvisible();
                setSimCardInvisible();
                break;
            case WIFI_EAP_METHOD_SIM:
            case WIFI_EAP_METHOD_AKA:
            case WIFI_EAP_METHOD_AKA_PRIME:
                WifiConfiguration config = mAccessPoint.getConfig();
                ArrayAdapter<String> eapSimAdapter = new ArrayAdapter<String>(
                         mContext, android.R.layout.simple_spinner_item,
                         mSimDisplayNames.toArray(new String[mSimDisplayNames.size()])
                );
                eapSimAdapter.setDropDownViewResource(
                              android.R.layout.simple_spinner_dropdown_item);
                mSimCardSpinner.setAdapter(eapSimAdapter);
                mView.findViewById(R.id.l_sim_card).setVisibility(View.VISIBLE);
                if(config != null){
                    mSimCardSpinner.setSelection(config.SIMNum-1);
                }
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setCaCertInvisible();
                setDomainInvisible();
                setUserCertInvisible();
                setPasswordInvisible();
                setIdentityInvisible();
                break;
        }

        if (mView.findViewById(R.id.l_ca_cert).getVisibility() != View.GONE) {
            String eapCertSelection = (String) mEapCaCertSpinner.getSelectedItem();
            if (eapCertSelection.equals(mDoNotValidateEapServerString)
                    || eapCertSelection.equals(mUnspecifiedCertString)) {
                // Domain suffix matching is not relevant if the user hasn't chosen a CA
                // certificate yet, or chooses not to validate the EAP server.
                setDomainInvisible();
            }
        }
    }

    private void setSimCardInvisible() {
        mView.findViewById(R.id.l_sim_card).setVisibility(View.GONE);
    }

    private void setIdentityInvisible() {
        mView.findViewById(R.id.l_identity).setVisibility(View.GONE);
        mPhase2Spinner.setSelection(Phase2.NONE);
    }

    private void setPhase2Invisible() {
        mView.findViewById(R.id.l_phase2).setVisibility(View.GONE);
        mPhase2Spinner.setSelection(Phase2.NONE);
    }

    private void setCaCertInvisible() {
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.GONE);
        setSelection(mEapCaCertSpinner, mUnspecifiedCertString);
    }

    private void setDomainInvisible() {
        mView.findViewById(R.id.l_domain).setVisibility(View.GONE);
        mEapDomainView.setText("");
    }

    private void setUserCertInvisible() {
        mView.findViewById(R.id.l_user_cert).setVisibility(View.GONE);
        setSelection(mEapUserCertSpinner, mUnspecifiedCertString);
    }

    private void setAnonymousIdentInvisible() {
        mView.findViewById(R.id.l_anonymous).setVisibility(View.GONE);
        mEapAnonymousView.setText("");
    }

    private void setPasswordInvisible() {
        mPasswordView.setText("");
        mView.findViewById(R.id.password_layout).setVisibility(View.GONE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.GONE);
    }

    private void showIpConfigFields() {
        WifiConfiguration config = null;

        mView.findViewById(R.id.ip_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.isSaved()) {
            config = mAccessPoint.getConfig();
        }

        if (mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) {
            mView.findViewById(R.id.staticip).setVisibility(View.VISIBLE);
            if (mIpAddressView == null) {
                mIpAddressView = (TextView) mView.findViewById(R.id.ipaddress);
                mIpAddressView.addTextChangedListener(this);
                mGatewayView = (TextView) mView.findViewById(R.id.gateway);
                mGatewayView.addTextChangedListener(this);
                mNetworkPrefixLengthView = (TextView) mView.findViewById(
                        R.id.network_prefix_length);
                mNetworkPrefixLengthView.addTextChangedListener(this);
                mDns1View = (TextView) mView.findViewById(R.id.dns1);
                mDns1View.addTextChangedListener(this);
                mDns2View = (TextView) mView.findViewById(R.id.dns2);
                mDns2View.addTextChangedListener(this);
            }
            if (config != null) {
                StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                if (staticConfig != null) {
                    if (staticConfig.ipAddress != null) {
                        mIpAddressView.setText(
                                staticConfig.ipAddress.getAddress().getHostAddress());
                        mNetworkPrefixLengthView.setText(Integer.toString(staticConfig.ipAddress
                                .getNetworkPrefixLength()));
                    }

                    if (staticConfig.gateway != null) {
                        mGatewayView.setText(staticConfig.gateway.getHostAddress());
                    }

                    Iterator<InetAddress> dnsIterator = staticConfig.dnsServers.iterator();
                    if (dnsIterator.hasNext()) {
                        mDns1View.setText(dnsIterator.next().getHostAddress());
                    }
                    if (dnsIterator.hasNext()) {
                        mDns2View.setText(dnsIterator.next().getHostAddress());
                    }
                }
            }
        } else {
            mView.findViewById(R.id.staticip).setVisibility(View.GONE);
        }
    }

    private void showProxyFields() {
        WifiConfiguration config = null;

        mView.findViewById(R.id.proxy_settings_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.isSaved()) {
            config = mAccessPoint.getConfig();
        }

        if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) {
            setVisibility(R.id.proxy_warning_limited_support, View.VISIBLE);
            setVisibility(R.id.proxy_fields, View.VISIBLE);
            setVisibility(R.id.proxy_pac_field, View.GONE);
            if (mProxyHostView == null) {
                mProxyHostView = (TextView) mView.findViewById(R.id.proxy_hostname);
                mProxyHostView.addTextChangedListener(this);
                mProxyPortView = (TextView) mView.findViewById(R.id.proxy_port);
                mProxyPortView.addTextChangedListener(this);
                mProxyExclusionListView = (TextView) mView.findViewById(R.id.proxy_exclusionlist);
                mProxyExclusionListView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyProperties = config.getHttpProxy();
                if (proxyProperties != null) {
                    mProxyHostView.setText(proxyProperties.getHost());
                    mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                    mProxyExclusionListView.setText(proxyProperties.getExclusionListAsString());
                }
            }
        } else if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_PAC) {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE);
            setVisibility(R.id.proxy_fields, View.GONE);
            setVisibility(R.id.proxy_pac_field, View.VISIBLE);

            if (mProxyPacView == null) {
                mProxyPacView = (TextView) mView.findViewById(R.id.proxy_pac);
                mProxyPacView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyInfo = config.getHttpProxy();
                if (proxyInfo != null) {
                    mProxyPacView.setText(proxyInfo.getPacFileUrl().toString());
                }
            }
        } else {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE);
            setVisibility(R.id.proxy_fields, View.GONE);
            setVisibility(R.id.proxy_pac_field, View.GONE);
        }
    }

    private void setVisibility(int id, int visibility) {
        final View v = mView.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    private void loadCertificates(
            Spinner spinner,
            String prefix,
            String noCertificateString,
            boolean showMultipleCerts,
            boolean showUsePreinstalledCertOption) {
        final Context context = mConfigUi.getContext();

        ArrayList<String> certs = new ArrayList<String>();
        certs.add(mUnspecifiedCertString);
        if (showMultipleCerts) {
            certs.add(mMultipleCertSetString);
        }
        if (showUsePreinstalledCertOption) {
            certs.add(mUseSystemCertsString);
        }
        certs.addAll(
                Arrays.asList(KeyStore.getInstance().list(prefix, android.os.Process.WIFI_UID)));
        certs.add(noCertificateString);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item,
                certs.toArray(new String[certs.size()]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; --i) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    public int getMode() {
        return mMode;
    }

    @Override
    public void afterTextChanged(Editable s) {
        mTextViewChangedHandler.post(new Runnable() {
                public void run() {
                    showWarningMessagesIfAppropriate();
                    enableSubmitIfAppropriate();
                }
            });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // work done in afterTextChanged
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // work done in afterTextChanged
    }

    @Override
    public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
        if (textView == mPasswordView) {
            if (id == EditorInfo.IME_ACTION_DONE && isSubmittable()) {
                mConfigUi.dispatchSubmit();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (view == mPasswordView) {
            if (keyCode == KeyEvent.KEYCODE_ENTER && isSubmittable()) {
                mConfigUi.dispatchSubmit();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        if (view.getId() == R.id.show_password) {
            int pos = mPasswordView.getSelectionEnd();
            mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT
                    | (isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                 : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            if (pos >= 0) {
                ((EditText) mPasswordView).setSelection(pos);
            }
        } else if (view.getId() == R.id.wifi_advanced_togglebox) {
            final View advancedToggle = mView.findViewById(R.id.wifi_advanced_toggle);
            final int toggleVisibility;
            final int stringID;
            if (isChecked) {
                toggleVisibility = View.VISIBLE;
                stringID = R.string.wifi_advanced_toggle_description_expanded;
            } else {
                toggleVisibility = View.GONE;
                stringID = R.string.wifi_advanced_toggle_description_collapsed;
            }
            mView.findViewById(R.id.wifi_advanced_fields).setVisibility(toggleVisibility);
            advancedToggle.setContentDescription(mContext.getString(stringID));
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mSecuritySpinner) {
            mAccessPointSecurity = position;
            showSecurityFields();
        } else if (parent == mEapMethodSpinner || parent == mEapCaCertSpinner) {
            showSecurityFields();
        } else if (parent == mProxySettingsSpinner) {
            showProxyFields();
        } else {
            showIpConfigFields();
        }
        showWarningMessagesIfAppropriate();
        enableSubmitIfAppropriate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    /**
     * Make the characters of the password visible if show_password is checked.
     */
    public void updatePassword() {
        TextView passwdView = (TextView) mView.findViewById(R.id.password);
        passwdView.setInputType(InputType.TYPE_CLASS_TEXT
                | (((CheckBox) mView.findViewById(R.id.show_password)).isChecked()
                   ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                   : InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
    }

    private void getSIMInfo() {
        int numOfSims;
        String displayname;
        mSubscriptionManager = SubscriptionManager.from(mContext);
        for(int i = 0; i < mTelephonyManager.getSimCount(); i++) {
            final SubscriptionInfo sir = mSubscriptionManager.
                  getActiveSubscriptionInfoForSimSlotIndex(i);
            if (sir != null) {
                displayname = String.valueOf(sir.getDisplayName());
            } else {
                displayname = mContext.getString(R.string.sim_editor_title, i + 1);
            }
            mSimDisplayNames.add(displayname);
        }
    }
}
