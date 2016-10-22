package info.tippingapp.firebase;

import android.app.Activity;
import android.content.Intent;
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
import com.paypal.android.MEP.PayPalInvoiceData;
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
    public static final int PAYPAL_REQUEST_CODE = 123;

    //Paypal Configuration Object
    private static PayPalConfiguration config = new PayPalConfiguration()
            // Start with mock environment.  When ready, switch to sandbox (ENVIRONMENT_SANDBOX)
            // or live (ENVIRONMENT_PRODUCTION)
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PayPalConfig.PAYPAL_CLIENT_ID);

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

        initLibrary();

        //start PayPalService
//        Intent intent = new Intent(this, PayPalService.class);
//
//        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
//
//        startService(intent);

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

//    private void showPayPalButton() {
//
//        // Generate the PayPal checkout button and save it for later use
//        PayPal pp = PayPal.getInstance();
//        launchPayPalButton = pp.getCheckoutButton(this, PayPal.BUTTON_278x43, CheckoutButton.TEXT_PAY);
//
//        // The OnClick listener for the checkout button
//        launchPayPalButton.setOnClickListener(this);
//
//        // Add the listener to the layout
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams (Toolbar.LayoutParams.WRAP_CONTENT,
//                Toolbar.LayoutParams.WRAP_CONTENT);
//        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//        params.bottomMargin = 10;
//        launchPayPalButton.setLayoutParams(params);
//        launchPayPalButton.setId(PAYPAL_BUTTON_ID);
//        ((RelativeLayout) findViewById(R.id.RelativeLayout01)).addView(launchPayPalButton);
//        ((RelativeLayout) findViewById(R.id.RelativeLayout01)).setGravity(Gravity.CENTER_HORIZONTAL);
//    }

    public void moveToThirdScreen(){
        secondScreenLabel.setVisibility(View.GONE);

        thirdScreenLabel.setVisibility(View.VISIBLE);
        receiverTitle.setText("To Andrew");
        receiverTitle.setVisibility(View.VISIBLE);
        btnPaymentSubmit.setVisibility(View.VISIBLE);
    }

    public void initLibrary() {
        PayPal pp = PayPal.getInstance();

        if (pp == null) {  // Test to see if the library is already initialized

            // This main initialization call takes your Context, AppID, and target server
            pp = PayPal.initWithAppID(this, "AeCGICbrQX9Isd4x1cpFXcc1tDgPk29VsH9xyUZhmBKHmN325abC6pUt8lmoUN_0C5hsXnPoGtnNA2OG", PayPal.ENV_NONE);

            // Required settings:

            // Set the language for the library
            pp.setLanguage("en_US");

            // Some Optional settings:

            // Sets who pays any transaction fees. Possible values are:
            // FEEPAYER_SENDER, FEEPAYER_PRIMARYRECEIVER, FEEPAYER_EACHRECEIVER, and FEEPAYER_SECONDARYONLY
            pp.setFeesPayer(PayPal.FEEPAYER_EACHRECEIVER);

            // true = transaction requires shipping
            pp.setShippingEnabled(true);

//            _paypalLibraryInit = true;
        }
    }

    public void getPayment(){

        // Create a basic PayPal payment
        PayPalPayment payment = new PayPalPayment();

        // Set the currency type
        payment.setCurrencyType("USD");

        // Set the recipient for the payment (can be a phone number)
        payment.setRecipient("");

        // Set the payment amount, excluding tax and shipping costs
        payment.setSubtotal(new BigDecimal(paymentAmount));

        // Set the payment type--his can be PAYMENT_TYPE_GOODS,
        // PAYMENT_TYPE_SERVICE, PAYMENT_TYPE_PERSONAL, or PAYMENT_TYPE_NONE
        payment.setPaymentType(PayPal.PAYMENT_TYPE_GOODS);

        // PayPalInvoiceData can contain tax and shipping amounts, and an
        // ArrayList of PayPalInvoiceItem that you can fill out.
        // These are not required for any transaction.
        PayPalInvoiceData invoice = new PayPalInvoiceData();

        // Set the tax amount
        invoice.setTax(new BigDecimal(0.1));

    }


//    private void getPayment() {
//
//        //Creating a paypalpayment
//        PayPalPayment payment = new PayPalPayment(new BigDecimal(String.valueOf(paymentAmount)), "USD", "Simplified Coding Fee",
//                PayPalPayment.PAYMENT_INTENT_SALE);
//
//        //Creating Paypal Payment activity intent
//        Intent intent = new Intent(this, PaymentActivity.class);
//
//        //putting the paypal configuration to the intent
//        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
//
//        //Puting paypal payment to the intent
//        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
//
//        //Starting the intent activity for result
//        //the request code will be used on the method onActivityResult
//        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //If the result is from paypal
        if (requestCode == PAYPAL_REQUEST_CODE) {

            //If the result is OK i.e. user has not canceled the payment
            if (resultCode == Activity.RESULT_OK) {
                //Getting the payment confirmation
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);

                //if confirmation is not null
                if (confirm != null) {
                    try {
                        //Getting the payment details
                        String paymentDetails = confirm.toJSONObject().toString(4);
                        Log.i("paymentExample", paymentDetails);

                        //Starting a new activity for the payment details and also putting the payment details with intent
                        startActivity(new Intent(this, ConfirmationActivity.class)
                                .putExtra("PaymentDetails", paymentDetails)
                                .putExtra("PaymentAmount", paymentAmount));

                    } catch (JSONException e) {
                        Log.e("paymentExample", "an extremely unlikely failure occurred: ", e);
                    }
                }
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

//    @Override
//    protected void onResume() {
//        super.onResume();
//        progressBar.setVisibility(View.GONE);
//    }

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
