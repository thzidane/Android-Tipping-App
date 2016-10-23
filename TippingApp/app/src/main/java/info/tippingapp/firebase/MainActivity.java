package info.tippingapp.firebase;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.paypal.android.MEP.CheckoutButton;
import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalActivity;
import com.paypal.android.MEP.PayPalAdvancedPayment;
import com.paypal.android.MEP.PayPalInvoiceData;
import com.paypal.android.MEP.PayPalReceiverDetails;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;

    //The views
    private Button btnPay, btnPaymentSubmit;
    private EditText editTextAmount;
    private TextView firstScreenLabel, enterAmountTitle, secondScreenLabel, thirdScreenLabel, receiverTitle;

    //Payment Amount
    private String paymentAmount;

    //Paypal intent request code to track onActivityResult method
    public static final int PAYPAL_REQUEST_CODE = 1;

    //Paypal Configuration Object
    private static PayPalConfiguration paypalConfig=new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID)
            .acceptCreditCards(true)
            // The following are only used in PayPalFuturePaymentActivity.
            .merchantName("Code_Crash")
            .merchantPrivacyPolicyUri(
                    Uri.parse("https://www.paypal.com/webapps/mpp/ua/privacy-full"))
            .merchantUserAgreementUri(
                    Uri.parse("https://www.paypal.com/webapps/mpp/ua/useragreement-full"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        btnPay = (Button)findViewById(R.id.btn_Pay);
        btnPaymentSubmit = (Button)findViewById(R.id.btn_payment_submit);
        editTextAmount = (EditText)findViewById(R.id.edit_text_amount);
        firstScreenLabel = (TextView)findViewById(R.id.first_screen_label);
        enterAmountTitle = (TextView)findViewById(R.id.enter_amount_title);
        secondScreenLabel = (TextView)findViewById(R.id.second_screen_label);
        thirdScreenLabel = (TextView)findViewById(R.id.third_screen_label);
        receiverTitle = (TextView)findViewById(R.id.receiver_title);

        secondScreenLabel.setVisibility(View.GONE);
        thirdScreenLabel.setVisibility(View.GONE);
        receiverTitle.setVisibility(View.GONE);
        btnPaymentSubmit.setVisibility(View.GONE);

        //get firebase auth instance
        auth = FirebaseAuth.getInstance();

        //get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //start PayPalService
        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, paypalConfig);
        startService(intent);

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    System.out.println("No user login!");
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };

        btnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstScreenLabel.setVisibility(View.GONE);
                editTextAmount.setVisibility(View.GONE);
                enterAmountTitle.setVisibility(View.GONE);
                btnPay.setVisibility(View.GONE);

                secondScreenLabel.setVisibility(View.VISIBLE);
                String tmp = editTextAmount.getText().toString();

                StringBuilder sb = new StringBuilder();
                for(int i=0;i<tmp.length();i++){
                    if(tmp.charAt(i)!=' '){
                        sb.append(tmp.charAt(i));
                    }
                }
                paymentAmount = sb.toString();
                sb = new StringBuilder();
                sb.append("Tip: $");
                sb.append(paymentAmount);
                thirdScreenLabel.setText(sb.toString());
                sb.deleteCharAt(3);
                btnPaymentSubmit.setText(sb.toString());

                moveToThirdScreen();
            }
        });

        btnPaymentSubmit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                thirdScreenLabel.setVisibility(View.GONE);
                receiverTitle.setVisibility(View.GONE);
                btnPaymentSubmit.setVisibility(View.GONE);
                getPayment();
            }
        });
    }

    public void moveToThirdScreen(){
        secondScreenLabel.setVisibility(View.GONE);

        thirdScreenLabel.setVisibility(View.VISIBLE);
        receiverTitle.setText("To Andrew");
        receiverTitle.setVisibility(View.VISIBLE);
        btnPaymentSubmit.setVisibility(View.VISIBLE);
    }

    public void getPayment(){

        PayPalPayment thingToBuy = new PayPalPayment(new BigDecimal(paymentAmount),"USD", "hao.tang.ecust-buyer@gmail.com",
                PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(MainActivity.this, PaymentActivity.class);

        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, paypalConfig);

        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, thingToBuy);

        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //If the result is from paypal
        if (requestCode == PAYPAL_REQUEST_CODE) {

            //If the result is OK i.e. user has not canceled the payment
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Payment done succesfully ", Toast.LENGTH_LONG).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.i("paymentExample", "The user canceled.");
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Log.i("paymentExample", "An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
            }
        }
    }

    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        //Handle item selection
        switch (item.getItemId()){
            case R.id.miAccount:
                return true;
            case R.id.miSignout:
                auth.signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            auth.removeAuthStateListener(authListener);
        }
    }

    //destroy PayPalService when app closes
    @Override
    public void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }
}
