package cn.cgm.flutter_nim.Helper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.model.CustomNotification;
import com.netease.nimlib.sdk.util.NIMUtil;

public class FlutterNIMHelper {

    public interface IMHelperNotificationCallback {
        void onEvent(CustomNotification message);
    }

    public interface IMLoginCallback {
        void onResult(boolean isSuccess);
    }

    private static IMHelperNotificationCallback imHelperNotificationCallback;

    // singleton
    private static FlutterNIMHelper instance;

    public static synchronized FlutterNIMHelper getInstance() {
        if (instance == null) {
            instance = new FlutterNIMHelper();
        }

        return instance;
    }

    private FlutterNIMHelper() {
        registerObservers(true);
    }


    public void registerNotificationCallback(IMHelperNotificationCallback cb) {
        imHelperNotificationCallback = cb;
    }

    public static void initIM(Context context) {
        if (NIMUtil.isMainProcess(context)) {
            // 注册自定义消息附件解析器
            NIMClient.getService(MsgService.class).registerCustomAttachmentParser(new FlutterNIMCustomAttachParser());
        }
    }

    /**
     * IM登录
     */
    public void doIMLogin(String account, String token, final IMLoginCallback loginCallback) {
        final String imAccount = account.toLowerCase();
        final String imToken = token.toLowerCase();

        LoginInfo info = new LoginInfo(imAccount, imToken);

        RequestCallback<LoginInfo> callback =
                new RequestCallback<LoginInfo>() {
                    @Override
                    public void onSuccess(LoginInfo param) {
                        saveLoginInfo(imAccount, imToken);

                        loginCallback.onResult(true);
                    }

                    @Override
                    public void onFailed(int code) {
                        Log.e("FlutterNIM", "im login failure" + code);

                        loginCallback.onResult(false);
                    }

                    @Override
                    public void onException(Throwable exception) {
                        Log.e("FlutterNIM", "im login error");
                    }
                };

        NIMClient.getService(AuthService.class).login(info)
                .setCallback(callback);
    }

    private static LoginInfo getLoginInfo(String account, String token) {
        if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(token)) {
            return new LoginInfo(account, token);
        } else {
            return null;
        }
    }

    private static void saveLoginInfo(final String account, final String token) {
        FlutterNIMPreferences.saveUserAccount(account);
        FlutterNIMPreferences.saveUserToken(token);
    }

    /**
     * IM登出
     */
    public void doIMLogout() {
        NIMClient.getService(AuthService.class).logout();
    }


    /**
     * ********************** 收消息，处理状态变化 ************************
     */
    private void registerObservers(boolean register) {
        MsgServiceObserve service = NIMClient.getService(MsgServiceObserve.class);

        // 监听自定义通知
        service.observeCustomNotification(customNotificationObserver, register);
    }

    // 接收自定义通知
    Observer<CustomNotification> customNotificationObserver = new Observer<CustomNotification>() {
        @Override
        public void onEvent(CustomNotification message) {
            // 在这里处理自定义通知。
            if (imHelperNotificationCallback != null) {
                imHelperNotificationCallback.onEvent(message);
            }
        }
    };
}
