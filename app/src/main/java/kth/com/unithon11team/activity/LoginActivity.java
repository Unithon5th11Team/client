package kth.com.unithon11team.activity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.vocketlist.android.roboguice.log.Ln;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.pedant.SweetAlert.SweetAlertDialog;
import kth.com.unithon11team.IPresneter.ILoginPresenter;
import kth.com.unithon11team.MainActivity;
import kth.com.unithon11team.Presenter.LoginPresenter;
import kth.com.unithon11team.R;
import kth.com.unithon11team.utils.DepthActivity;
import rx.Observable;
import rx.Subscriber;

public class LoginActivity extends DepthActivity implements GoogleApiClient.ConnectionCallbacks
		, GoogleApiClient.OnConnectionFailedListener {


	@BindView(R.id.activity_login_google_btn) ImageButton login_google_btn;
	@BindView(R.id.activity_login_signIn_btn) Button login_signIn_btn;
	@BindView(R.id.activity_login_signUp_btn) Button login_signUp_btn;
	@BindView(R.id.email_et) MaterialEditText email_et;
	@BindView(R.id.pw_et) MaterialEditText pw_et;


	private static final String EXTRA_MESSAGE = "com.buttering.roler";
	private static final String TAG = "LogInActivity";
	private static final String OAUTH_CLIENT_ID = "nfRec7uCc36x_KoxxTzC";
	private static final String OAUTH_CLIENT_SECRET = "dPDGbaB_3V";
	private static final String OAUTH_CLIENT_NAME = "Roler";

	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final int GET_ACCOUNT = 111;
	private static final int single_top_activity = 999;

	private OAuthLogin mOAuthLoginModule;
	private GoogleApiClient mGoogleApiClient;
	private ILoginPresenter loginPresenter;
	private SweetAlertDialog materialDialog;


	private boolean isEmptyEmail;
	private boolean isEmptyPwd;


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case GET_ACCOUNT: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					//reload my activity with permission granted or use the features what required the permission
				} else {
					Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	private void checkThePemission() {
		if (Build.VERSION.SDK_INT > 22) {
			boolean hasPermission = (ContextCompat.checkSelfPermission(this,
					Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED);
			if (!hasPermission) {
				ActivityCompat.requestPermissions(this,
						new String[]{
								android.Manifest.permission.GET_ACCOUNTS}, GET_ACCOUNT);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		ButterKnife.bind(this);

		checkPlayServices();

		checkThePemission();

		checkTheRegularExpression();

		checkTheHilightEditText();

		loginPresenter = new LoginPresenter(this);
//		signUpPresenter = new SignUpPresenter();

		initLoginSetting();
	}


	private void initLoginSetting() {

		mOAuthLoginModule = OAuthLogin.getInstance();
		mOAuthLoginModule.init(
				getApplicationContext()
				, OAUTH_CLIENT_ID
				, OAUTH_CLIENT_SECRET
				, OAUTH_CLIENT_NAME
		);

		OAuthLoginButton mOAuthLoginButton = (OAuthLoginButton) findViewById(R.id.buttonOAuthLoginImg);
		mOAuthLoginButton.setOAuthLoginHandler(mOAuthLoginHandler);

	}


	private void checkTheHilightEditText() {

		email_et.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(1000);
							email_et.setFloatingLabelText(isHignlightValid(s.toString()));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}).start();

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});

	}

	private String isHignlightValid(String email) {

		if (email == null || email.isEmpty()) {
			return getString(R.string.join_empty_email);
		}
		if (email.length() > 60) {
			return getString(R.string.join_too_long_email);
		}

		if (!isEmailValid(email)) {
			return getString(R.string.join_invalid_email);
		}

		return getString(R.string.welcome_roler_title);
	}


	private boolean isEmailValid(String email) {
		boolean isValid = false;

		String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";

		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(email);
		if (matcher.matches()) {
			isValid = true;
		}
		return isValid;
	}

	private void checkTheRegularExpression() {

		Observable<CharSequence> emailObservable = RxTextView.textChanges(email_et);
		emailObservable.map(charSequence -> charSequence.length() > 0).subscribe(new Subscriber<Boolean>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(Boolean aBoolean) {
				isEmptyEmail = aBoolean;
				if (aBoolean && isEmptyPwd) {
					login_signIn_btn.setBackground(ContextCompat.getDrawable(LoginActivity.this, R.drawable.sign_in_on_round_btn));
				} else {
					login_signIn_btn.setBackground(ContextCompat.getDrawable(LoginActivity.this, R.drawable.sign_in_off_round_btn));
				}

			}
		});

		Observable<CharSequence> pwdObservable = RxTextView.textChanges(pw_et);
		pwdObservable.map(charSequence -> charSequence.length() > 0).subscribe(new Subscriber<Boolean>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(Boolean aBoolean) {
				isEmptyPwd = aBoolean;
				if (aBoolean && isEmptyEmail) {
					login_signIn_btn.setBackground(ContextCompat.getDrawable(LoginActivity.this, R.drawable.sign_in_on_round_btn));
				} else {
					login_signIn_btn.setBackground(ContextCompat.getDrawable(LoginActivity.this, R.drawable.sign_in_off_round_btn));
				}

			}
		});


	}


	private boolean checkPlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode)) {
				apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
						.show();
			} else {
				Log.i("df", "This device is not supported.");
			}
			return false;
		}
		return true;
	}


	public void onConnected(Bundle bundle) {
		Log.d(TAG, "구글 플레이 연결이 되었습니다.");

		if (!mGoogleApiClient.isConnected() || Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) == null) {

			Log.d(TAG, "onConnected 연결 실패");

		} else {
			Log.d(TAG, "onConnected 연결 성공");

			Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
		/*		 TODO: Consider calling
				    ActivityCompat#requestPermissions
				 here to request the missing permissions, and then overriding
				   public void onRequestPermissionsResult(int requestCode, String[] permissions,
				                                          int[] grantResults)
				 to handle the case where the user grants the permission. See the documentation
				 for ActivityCompat#requestPermissions for more details.
				return;
			*/
			}

			if (currentPerson.hasImage()) {
				Log.d(TAG, "이미지 경로는 : " + currentPerson.getImage().getUrl());
			}
			if (currentPerson.hasDisplayName()) {

				Log.d(TAG, "google+ name  : " + currentPerson.getDisplayName());
				Log.d(TAG, "google+ id : " + currentPerson.getId());
				Log.d(TAG, "google+ id : " + currentPerson.getAboutMe());
				Log.d(TAG, "google+ id : " + currentPerson.getUrl());

				String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
				String name = currentPerson.getDisplayName();
				String pwd = String.valueOf(currentPerson.getId());

				showLoadingBar();


			}
		}
	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.d(TAG, "연결 에러 " + connectionResult);

		if (connectionResult.hasResolution()) {

			Log.e(TAG,
					String.format(
							"Connection to Play Services Failed, error: %d, reason: %s",
							connectionResult.getErrorCode(),
							connectionResult.toString()));
			try {
				//이게 핵심?
				connectionResult.startResolutionForResult(this, PLAY_SERVICES_RESOLUTION_REQUEST);
			} catch (IntentSender.SendIntentException e) {
				Log.e(TAG, e.toString(), e);
				//재시도
				mGoogleApiClient.connect();
			}
		} else {
			Toast.makeText(getApplicationContext(), "이미 로그인 중", Toast.LENGTH_SHORT).show();
		}
	}


	public void showLoadingBar() {
		materialDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
		materialDialog.getProgressHelper().setBarColor(this.getResources().getColor(R.color.dialog_color));
		materialDialog.setTitleText(getString(R.string.loading_dialog_title));
		materialDialog.setCancelable(false);
		materialDialog.show();
	}


	public void hideLoadingBar() {
		if (materialDialog != null)
			materialDialog.dismiss();
	}

	@OnClick(R.id.activity_login_google_btn)
	public void loginGoogleOnClick() {

		checkThePemission();

		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Plus.API)
				.addScope(Plus.SCOPE_PLUS_PROFILE)
				.build();

		mGoogleApiClient.connect();

	}

	@Override
	protected void onPause() {
		if (mGoogleApiClient != null) {
			mGoogleApiClient.disconnect();
		}
		super.onPause();
	}

	private void getNaverUserInfo(String accessToken) {

		String token = accessToken;// 네이버 로그인 접근 토큰;
		String header = "Bearer " + token; // Bearer 다음에 공백 추가

		try {
			String apiURL = "https://openapi.naver.com/v1/nid/me";
			URL url = new URL(apiURL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", header);
			int responseCode = con.getResponseCode();
			BufferedReader br;
			if (responseCode == 200) { // 정상 호출
				br = new BufferedReader(new InputStreamReader(con.getInputStream()));
			} else {  // 에러 발생
				br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
			}
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = br.readLine()) != null) {
				response.append(inputLine);
			}
			br.close();

			System.out.println(response.toString());
			String res = response.toString();
			JsonObject ja = new JsonParser().parse(res).getAsJsonObject();
			String name = ja.getAsJsonObject("response").getAsJsonPrimitive("nickname").getAsString();
			String email = ja.getAsJsonObject("response").getAsJsonPrimitive("email").getAsString();
			String pwd = ja.getAsJsonObject("response").getAsJsonPrimitive("id").getAsString();


		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
		@Override
		public void run(boolean success) {
			if (success) {

				String accessToken = mOAuthLoginModule.getAccessToken(LoginActivity.this);
				String refreshToken = mOAuthLoginModule.getRefreshToken(LoginActivity.this);
				long expiresAt = mOAuthLoginModule.getExpiresAt(LoginActivity.this);
				String tokenType = mOAuthLoginModule.getTokenType(LoginActivity.this);

				showLoadingBar();

				new Thread() {
					@Override
					public void run() {

						getNaverUserInfo(accessToken);

					}

				}.start(); //start thread


			} else {

				String errorCode = mOAuthLoginModule.getLastErrorCode(getApplicationContext()).getCode();
				String errorDesc = mOAuthLoginModule.getLastErrorDesc(getApplicationContext());

				Ln.d(errorCode, errorDesc);
			}
		}


	};

	private void goToPlanActivity() {
		Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}


	@OnClick(R.id.activity_login_signUp_btn)
	public void signUpOnClick() {
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

	@OnClick(R.id.activity_login_signIn_btn)
	public void signInOnClick() {

		String email = email_et.getText().toString();
		String pwd = pw_et.getText().toString();

		if (isValid(email, pwd)) {

			showLoadingBar();

			loginPresenter.signIn(email, pwd)
					.subscribe(new Subscriber<Void>() {
						@Override
						public void onCompleted() {

						}

						@Override
						public void onError(Throwable e) {
							hideLoadingBar();
							Toast.makeText(LoginActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
							e.printStackTrace();
						}

						@Override
						public void onNext(Void aVoid) {

							hideLoadingBar();
//							SharePrefUtil.putSharedPreference(getString(R.string.IS_LOGIN_KEY), true);
							goToPlanActivity();

						}
					});
		}

	}


	private boolean isValid(String email, String passwd) {

		if (email == null || email.isEmpty()) {
			Toast.makeText(getApplicationContext(), getString(R.string.join_empty_email),
					Toast.LENGTH_SHORT).show();
			return false;
		}
		if (email.length() > 60) {
			Toast.makeText(getApplicationContext(), getString(R.string.join_too_long_email),
					Toast.LENGTH_SHORT).show();
			return false;
		}

		if (!isEmailValid(email)) {
			Toast.makeText(getApplicationContext(), getString(R.string.join_invalid_email),
					Toast.LENGTH_SHORT).show();
			return false;
		}


		if (passwd == null || passwd.isEmpty()) {
			Toast.makeText(getApplicationContext(), getString(R.string.join_empty_pwd),
					Toast.LENGTH_SHORT).show();
			return false;
		}

		if (passwd.length() < 8) {
			Toast.makeText(getApplicationContext(), getString(R.string.join_too_short_pwd),
					Toast.LENGTH_SHORT).show();
			return false;
		}


		return true;
	}


}