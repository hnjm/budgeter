package com.example.ammarhasan.budgeter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ammar Hasan 150454388 April 2018
 * Class Purpose: This class contains the  RecordTransactionActivity
 * activity functionality, handles addition of transcations
 */
public class RecordTransactionActivity extends AppCompatActivity {

    private EditText mAmountText;
    private FirebaseAuth mFirebaseAuth;
    private Spinner mBudgetSpinner;
    private boolean credit;

    // reference to interact with firebase
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_transaction);

        // find all buttons and fields
        Button recordTransactionBtn = findViewById(R.id.button_record_transcation);
        Switch creditDebitSwitch = findViewById(R.id.switch_credit_debit);
        mAmountText = findViewById(R.id.text_low_bank_warn_value);
        mBudgetSpinner = findViewById(R.id.spinner_budget);


        // get firebase database reference and Authentication object
        databaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();

        credit = false; // set credit default
        mBudgetSpinner.setVisibility(View.VISIBLE); // hide budget spinner if debit (default)

        // attach listener to switch
        creditDebitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    credit = true;
                    mBudgetSpinner.setVisibility(View.GONE); // hide budget spinner if credit
                } else {

                    credit = false;
                    mBudgetSpinner.setVisibility(View.VISIBLE); // hide budget spinner if credit
                }
            }
        });


        //  get current user to read database
        final FirebaseUser user = mFirebaseAuth.getCurrentUser();

        // Attach a listener to read the data (user info)
        databaseReference.child(user.getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        // get user info and update
                        User userInfo = dataSnapshot.getValue(User.class);


                        // add content to drop down

                        // create list of items by reading database
                        List<String> budgetNames = new ArrayList<>();

                        // fill string list
                        for (Budget b : userInfo.getBudgets()) {
                            budgetNames.add(b.getName());
                        }

                        // put list in array
                        ArrayAdapter<String> adapter = new ArrayAdapter<>
                                (RecordTransactionActivity.this,
                                        android.R.layout.simple_spinner_dropdown_item, budgetNames);

                        adapter.setDropDownViewResource
                                (android.R.layout.simple_spinner_dropdown_item);

                        mBudgetSpinner.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(RecordTransactionActivity.this,
                                "The read failed: " + databaseError.getCode(),
                                Toast.LENGTH_LONG).show();
                    }
                });

    }

    /**
     * This method is executed when button_record_transcation
     * is clicked to record user transaction
     * @param view the view provided by the button
     */
    public void recordTransaction(View view) {

        // If a fields is empty, alert user and return
        if(mAmountText.getText().toString().trim().isEmpty()){
            Toast.makeText(RecordTransactionActivity.this,
                    getResources().getString(R.string.empty_field_error), Toast.LENGTH_LONG).show();
            return;
        }

        // get recorded amount
        final double amount = Double.parseDouble(mAmountText.getText().toString());

        // get current user
        final FirebaseUser user = mFirebaseAuth.getCurrentUser();

        // Attach a listener to read the data (user info)
        databaseReference.child(user.getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // get user info and update
                User userInfo = dataSnapshot.getValue(User.class);

                // using a transaction update
                if (credit){
                    // check and report any errors
                    try{
                        userInfo.carryTransaction(null,amount,credit);
                    }

                    catch (IllegalArgumentException e){
                        Toast.makeText(RecordTransactionActivity.this, e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                } else{ // else substract

                    // check budget list (if null exit and alert user)
                    if(userInfo.getBudgets().size() == 0){
                        Toast.makeText(RecordTransactionActivity.this,
                                getResources().getString(R.string.transaction_no_budget_error),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }

                    else {
                        // get the budget as well if it's set to debit
                        Budget budget =
                                userInfo.findBudget(mBudgetSpinner.getSelectedItem().toString());

                        // Check and report any errors
                        try {
                            userInfo.carryTransaction(budget, amount, credit);

                            // alert user if bank amount is low
                            if(userInfo.getBankAmount() <= userInfo.getLowBankWarning()){
                                Toast.makeText(RecordTransactionActivity.this,
                                        getResources().getString(R.string.low_budget_warn),
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        catch (IllegalArgumentException e){
                            Toast.makeText(RecordTransactionActivity.this, e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }

                // update value in database using user id
                databaseReference.child(user.getUid()).setValue(userInfo);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(RecordTransactionActivity.this,
                        getResources().getString(R.string.db_error_text)
                                + databaseError.getCode(),
                        Toast.LENGTH_LONG).show();
            }
        });

        // return back to home fragment in the navigation activity
        finish();
    }
}
