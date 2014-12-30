package com.example.wcl.test_weiboshare;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.utils.LogUtil;

import java.text.SimpleDateFormat;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = "DEBUG-WCL: " + MainActivity.class.getSimpleName();

    private Button mShareJumpWeiboButton; // 跳转分享到微博的按钮
    private Button mShareApiWeiboButton; // API分享到微博的按钮

    private EditText mShareEditText; // 分享内容输入框

    private TextView mTokenText; // Token信息

    private IWeiboShareAPI mWeiboShareAPI; // 微博分享实例

    private Oauth2AccessToken mAccessToken; // 授权访问令牌
    private AuthInfo mAuthInfo; // Sso授权信息
    private SsoHandler mSsoHandler; // Sso授权实例
    private StatusesAPI mStatusesAPI; // 用于获取微博信息流等操作的API

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mShareJumpWeiboButton = (Button) findViewById(R.id.share_jump_weibo_button);
        mShareApiWeiboButton = (Button) findViewById(R.id.share_api_weibo_button);
        mShareEditText = (EditText) findViewById(R.id.share_message_edit_text);
        mTokenText = (TextView) findViewById(R.id.token_text_view);

        mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(this, Constants.APP_KEY);
        mWeiboShareAPI.registerApp();
        if (savedInstanceState != null) {
            mWeiboShareAPI.handleWeiboResponse(getIntent(), new IWeiboHandler.Response() {
                @Override
                public void onResponse(BaseResponse baseResponse) {
                    switch (baseResponse.errCode) {
                        case WBConstants.ErrorCode.ERR_OK:
                            Toast.makeText(MainActivity.this,
                                    R.string.weibosdk_demo_toast_share_success,
                                    Toast.LENGTH_LONG).show();
                            break;
                        case WBConstants.ErrorCode.ERR_CANCEL:
                            Toast.makeText(MainActivity.this,
                                    R.string.weibosdk_demo_toast_share_canceled,
                                    Toast.LENGTH_LONG).show();
                            break;
                        case WBConstants.ErrorCode.ERR_FAIL:
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.weibosdk_demo_toast_share_failed) + "Error Message: " + baseResponse.errMsg,
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            });
        }

        mAuthInfo = new AuthInfo(this, Constants.APP_KEY, Constants.REDIRECT_URL, Constants.SCOPE);
        mSsoHandler = new SsoHandler(MainActivity.this, mAuthInfo);
//        mAccessToken = AccessTokenKeeper.readAccessToken(this);

        mShareJumpWeiboButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "点击跳转跳转分享按钮");
                sendMessage();
            }
        });

        mShareApiWeiboButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "点击API分享按钮");

                // 授权
                mSsoHandler.authorize(new AuthListener());

//                // 获取当前已保存过的 Token
//                mAccessToken = AccessTokenKeeper.readAccessToken(MainActivity.this);
//                // 对statusAPI实例化
//                mStatusesAPI = new StatusesAPI(MainActivity.this, Constants.APP_KEY, mAccessToken);
//
//                mStatusesAPI.update(mShareEditText.getText().toString(), null, null, new RequestListener() {
//                    @Override
//                    public void onComplete(String response) {
//                        if (!TextUtils.isEmpty(response)) {
//                            LogUtil.i(TAG, response);
//                            if (response.startsWith("{\"statuses\"")) {
//                                // 调用 StatusList#parse 解析字符串成微博列表对象
//                                StatusList statuses = StatusList.parse(response);
//                                if (statuses != null && statuses.total_number > 0) {
//                                    Toast.makeText(MainActivity.this,
//                                            "获取微博信息流成功, 条数: " + statuses.statusList.size(),
//                                            Toast.LENGTH_LONG).show();
//                                }
//                            } else if (response.startsWith("{\"created_at\"")) {
//                                // 调用 Status#parse 解析字符串成微博对象
//                                Status status = Status.parse(response);
//                                Toast.makeText(MainActivity.this,
//                                        "发送一送微博成功, id = " + status.id,
//                                        Toast.LENGTH_LONG).show();
//                            } else {
//                                Toast.makeText(MainActivity.this, response, Toast.LENGTH_LONG).show();
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onWeiboException(WeiboException e) {
//                        LogUtil.e(TAG, e.getMessage());
//                        ErrorInfo info = ErrorInfo.parse(e.getMessage());
//                        Toast.makeText(MainActivity.this, info.toString(), Toast.LENGTH_LONG).show();
//                    }
//                });
            }
        });
    }

    private void sendMessage() {

        // 1. 初始化微博的分享消息
        WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
        weiboMessage.textObject = getTextObj();

        // 2. 初始化从第三方到微博的消息请求
        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
        // 用transaction唯一标识一个请求
        request.transaction = String.valueOf(System.currentTimeMillis());
        request.multiMessage = weiboMessage;

        // 3. 发送请求消息到微博，唤起微博分享界面
        AuthInfo authInfo = new AuthInfo(this, Constants.APP_KEY, Constants.REDIRECT_URL, Constants.SCOPE);
        Oauth2AccessToken accessToken = AccessTokenKeeper.readAccessToken(getApplicationContext());
        String token = "";
        if (accessToken != null) {
            token = accessToken.getToken();
        }

        mWeiboShareAPI.sendRequest(this, request, authInfo, token, new WeiboAuthListener() {
            @Override
            public void onWeiboException(WeiboException arg0) {
            }
            @Override
            public void onComplete(Bundle bundle) {
                // TODO Auto-generated method stub
                Oauth2AccessToken newToken = Oauth2AccessToken.parseAccessToken(bundle);
                AccessTokenKeeper.writeAccessToken(getApplicationContext(), newToken);
                Toast.makeText(MainActivity.this,
                        "onAuthorizeComplete token = " + newToken.getToken(), Toast.LENGTH_LONG).show();
            }
            @Override
            public void onCancel() {
            }
        });
    }

    private void updateTokenView(boolean hasExisted) {
        String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(
                new java.util.Date(mAccessToken.getExpiresTime()));
        String format = getString(R.string.weibosdk_demo_token_to_string_format_1);
        mTokenText.setText(String.format(format, mAccessToken.getToken(), date));

        String message = String.format(format, mAccessToken.getToken(), date);
        if (hasExisted) {
            message = getString(R.string.weibosdk_demo_token_has_existed) + "\n" + message;
        }
        mTokenText.setText(message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private TextObject getTextObj() {
        TextObject textObject = new TextObject();
        textObject.text = mShareEditText.getText().toString();
        return textObject;
    }

    /**
     * 微博认证授权回调类。
     * 1. SSO 授权时，需要在 {@link #onActivityResult} 中调用 {@link SsoHandler#authorizeCallBack} 后，
     *    该回调才会被执行。
     * 2. 非 SSO 授权时，当授权结束后，该回调就会被执行。
     * 当授权成功后，请保存该 access_token、expires_in、uid 等信息到 SharedPreferences 中。
     */
    class AuthListener implements WeiboAuthListener {

        @Override
        public void onComplete(Bundle values) {
            // 从 Bundle 中解析 Token
            mAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mAccessToken.isSessionValid()) {
                // 显示 Token
                updateTokenView(false);

                // 保存 Token 到 SharedPreferences
                AccessTokenKeeper.writeAccessToken(MainActivity.this, mAccessToken);
                Toast.makeText(MainActivity.this,
                        R.string.weibosdk_demo_toast_auth_success, Toast.LENGTH_SHORT).show();
            } else {
                // 以下几种情况，您会收到 Code：
                // 1. 当您未在平台上注册的应用程序的包名与签名时；
                // 2. 当您注册的应用程序包名与签名不正确时；
                // 3. 当您在平台上注册的包名和签名与您当前测试的应用的包名和签名不匹配时。
                String code = values.getString("code");
                String message = getString(R.string.weibosdk_demo_toast_auth_failed);
                if (!TextUtils.isEmpty(code)) {
                    message = message + "\nObtained the code: " + code;
                }
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onCancel() {
            Toast.makeText(MainActivity.this,
                    R.string.weibosdk_demo_toast_auth_canceled, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Toast.makeText(MainActivity.this,
                    "Auth exception : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
