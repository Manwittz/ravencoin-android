package com.ravencoin.presenter.activities.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;

import com.ravencoin.RavenApp;
import com.ravencoin.presenter.activities.DisabledActivity;
import com.ravencoin.presenter.activities.intro.IntroActivity;
import com.ravencoin.presenter.activities.intro.RecoverActivity;
import com.ravencoin.presenter.activities.intro.WriteDownActivity;
import com.ravencoin.tools.animation.BRAnimator;
import com.ravencoin.tools.manager.BRApiManager;
import com.ravencoin.tools.manager.InternetManager;
import com.ravencoin.tools.security.AuthManager;
import com.ravencoin.tools.security.BRKeyStore;
import com.ravencoin.wallet.wallets.util.CryptoUriParser;
import com.ravencoin.tools.security.PostAuth;
import com.ravencoin.tools.threads.executor.BRExecutor;
import com.ravencoin.tools.util.BRConstants;
import com.ravencoin.wallet.WalletsMaster;
import com.platform.HTTPServer;
import com.platform.tools.BRBitId;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/23/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class RActivity extends Activity {
    private static final String TAG = RActivity.class.getName();
    public static final Point screenParametersPoint = new Point();


    static {
        System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        RavenApp.activityCounter.decrementAndGet();
        RavenApp.onStop(this);
    }

    @Override
    protected void onResume() {
        init(this);
        super.onResume();
        RavenApp.backgroundedTime = 0;

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        // 123 is the qrCode result
        switch (requestCode) {

            case BRConstants.PAY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPublishTxAuth(RActivity.this, true);
                        }
                    });
                }
                break;
            case BRConstants.REQUEST_PHRASE_BITID:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onBitIDAuth(RActivity.this, true);
                        }
                    });

                }
                break;

            case BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPaymentProtocolRequest(RActivity.this, true);
                        }
                    });

                }
                break;

            case BRConstants.CANARY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onCanaryCheck(RActivity.this, true);
                        }
                    });
                } else {
                    finish();
                }
                break;

            case BRConstants.SHOW_PHRASE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPhraseCheckAuth(RActivity.this, true);
                        }
                    });
                }
                break;
            case BRConstants.PROVE_PHRASE_REQUEST:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onPhraseProveAuth(RActivity.this, true);
                        }
                    });
                }
                break;
            case BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onRecoverWalletAuth(RActivity.this, true);
                        }
                    });
                } else {
                    finish();
                }
                break;

            case BRConstants.SCANNER_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            String result = data.getStringExtra("result");
                            if (CryptoUriParser.isCryptoUrl(RActivity.this, result))
                                CryptoUriParser.processRequest(RActivity.this, result,
                                        WalletsMaster.getInstance(RActivity.this).getCurrentWallet(RActivity.this));
                            else if (BRBitId.isBitId(result))
                                BRBitId.signBitID(RActivity.this, result, null);
                            else
                                Log.e(TAG, "onActivityResult: not bitcoin address NOR bitID");
                        }
                    });

                }
                break;

            case BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            PostAuth.getInstance().onCreateWalletAuth(RActivity.this, true);
                        }
                    });

                } else {
                    Log.e(TAG, "WARNING: resultCode != RESULT_OK");
                    WalletsMaster m = WalletsMaster.getInstance(RActivity.this);
                    m.wipeWalletButKeystore(this);
                    finish();
                }
                break;

        }
    }

    public void init(Activity app) {
        //set status bar color
//        ActivityUTILS.setStatusBarColor(app, android.R.color.transparent);
        InternetManager.getInstance();
        if (!(app instanceof IntroActivity || app instanceof RecoverActivity || app instanceof WriteDownActivity))
            BRApiManager.getInstance().startTimer(app);
        //show wallet locked if it is
        if (!ActivityUTILS.isAppSafe(app))
            if (AuthManager.getInstance().isWalletDisabled(app))
                AuthManager.getInstance().setWalletDisabled(app);

        RavenApp.activityCounter.incrementAndGet();
        RavenApp.setBreadContext(app);

        if (!HTTPServer.isStarted())
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    HTTPServer.startServer();
                }
            });

        lockIfNeeded(this);

    }

    private void lockIfNeeded(Activity app) {
        //lock wallet if 3 minutes passed
        if (RavenApp.backgroundedTime != 0
                && ((System.currentTimeMillis() - RavenApp.backgroundedTime) >= 180 * 1000)
                && !(app instanceof DisabledActivity)) {
            if (!BRKeyStore.getPinCode(app).isEmpty()) {
                Log.e(TAG, "lockIfNeeded: " + RavenApp.backgroundedTime);
                BRAnimator.startBreadActivity(app, true);
            }
        }

    }

}
