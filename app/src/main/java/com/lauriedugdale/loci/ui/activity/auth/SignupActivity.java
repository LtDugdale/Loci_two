package com.lauriedugdale.loci.ui.activity.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.lauriedugdale.loci.data.UserDatabase;
import com.lauriedugdale.loci.utils.DataUtils;
import com.lauriedugdale.loci.R;
import com.lauriedugdale.loci.ui.activity.MainActivity;

public class SignupActivity extends AppCompatActivity {

    private EditText mInputUsername;
    private EditText mInputEmail;
    private EditText mInputPassword;
    private Button mBtnSignIn;
    private Button mBtnSignUp;
    private Button mBtnResetPassword;
    private ProgressBar mProgressBar;
    private FirebaseAuth mAuth;
    private UserDatabase mUserDatabase;

    private String mUsername;
    private String mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // access database methods
        mUserDatabase = new UserDatabase(this);

        //Get Firebase auth instance
        mAuth = FirebaseAuth.getInstance();

        mBtnSignIn = (Button) findViewById(R.id.sign_in_button);
        mBtnSignUp = (Button) findViewById(R.id.sign_up_button);
        mInputUsername = (EditText) findViewById(R.id.username);
        mInputEmail = (EditText) findViewById(R.id.email);
        mInputPassword = (EditText) findViewById(R.id.password);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mBtnResetPassword = (Button) findViewById(R.id.btn_reset_password);

        mBtnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, ResetPasswordActivity.class));
            }
        });

        mBtnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mBtnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mUsername = mInputUsername.getText().toString().trim();
                mEmail = mInputEmail.getText().toString().trim();
                String password = mInputPassword.getText().toString().trim();

                if (TextUtils.isEmpty(mEmail)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show();
                    return;
                }

                mProgressBar.setVisibility(View.VISIBLE);
                //create user
                mAuth.createUserWithEmailAndPassword(mEmail, password)
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Toast.makeText(SignupActivity.this, "createUserWithEmail:onComplete:" + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                                mProgressBar.setVisibility(View.GONE);
                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Toast.makeText(SignupActivity.this, "Authentication failed." + task.getException(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        startActivity(new Intent(SignupActivity.this, MainActivity.class));

                        mUserDatabase.uploadNewUser(mUsername, mEmail);
                        finish();
                    }
                });

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mProgressBar.setVisibility(View.GONE);
    }
}