package net.cloudseat.fingerprint;

import android.app.DialogFragment;
import android.hardware.fingerprint.FingerprintManager;

import android.os.CancellationSignal;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.crypto.Cipher;

public class FingerprintDialogFragment extends DialogFragment {

    private FingerprintManager fingerprintManager;
    private CancellationSignal cancellationSignal;

    private Cipher cipher;
    private TextView message;

    // 是否用户主动取消
    private boolean isSelfCancelled;

    public void setCipher(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fingerprintManager = getContext().getSystemService(FingerprintManager.class);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        String pkg = FingerprintPlugin.packageName;
        int layoutId = getResources().getIdentifier("fingerprint_dialog", "layout", pkg);
        int messageId = getResources().getIdentifier("message", "id", pkg);
        int cancelId = getResources().getIdentifier("cancel", "id", pkg);

        View layout = inflater.inflate(layoutId, container, false);
        message = (TextView) layout.findViewById(messageId);

        TextView cancel = (TextView) layout.findViewById(cancelId);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
                stopListening();
            }
        });
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        startListening();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListening();
    }

    ///////////////////////////////////////////////////////
    // 私有方法
    ///////////////////////////////////////////////////////

    /**
     * 开始监听指纹输入
     */
    private void startListening() {
        isSelfCancelled = false;
        cancellationSignal = new CancellationSignal();

        fingerprintManager.authenticate(new FingerprintManager.CryptoObject(cipher), cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                if (!isSelfCancelled) {
                    message.setTextColor(0xffff4455);
                    message.setText(errString);
                    if (errorCode == FingerprintManager.FINGERPRINT_ERROR_LOCKOUT) {
                        dismissAllowingStateLoss();
                    }
                }
            }

            @Override
            public void onAuthenticationFailed() {
                message.setTextColor(0xffff4455);
                message.setText("指纹认证失败");
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                message.setTextColor(0xff333333);
                message.setText(helpString);
            }

            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                message.setTextColor(0xff00cc77);
                message.setText("指纹认证成功");

                // 延迟 500 毫秒执行回调
                message.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintPlugin.onAuthenticated();
                        dismissAllowingStateLoss();
                    }
                }, 500);
            }
        }, null);
    }

    /**
     * 停止监听指纹输入
     */
    private void stopListening() {
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            cancellationSignal = null;
            isSelfCancelled = true;
        }
    }

}
