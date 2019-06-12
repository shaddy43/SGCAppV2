package com.example.shaya.sgcapp.UI;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.example.shaya.sgcapp.Authentication;
import com.example.shaya.sgcapp.GroupsConfig;
import com.example.shaya.sgcapp.LocalDatabaseHelper;
import com.example.shaya.sgcapp.R;
import com.example.shaya.sgcapp.domain.sharedPreferences.SharedPreferencesConfig;
import com.example.shaya.sgcapp.domain.Validation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class Main2Activity extends AppCompatActivity {

    private TabLayout tLayout;
    private ViewPager vPager;

    private TabsPagerAdapter tabsPagerAdapter;
    private DatabaseReference ref;

    private FirebaseAuth mAuth;
    private String currentUserId;

    private Validation validation;
    private GroupsConfig config;

    private SharedPreferencesConfig sp;
    //private LocalDatabaseHelper db;
    //private String defaultGroupPicUrl = "https://firebasestorage.googleapis.com/v0/b/sgcapp-8dcbb.appspot.com/o/group_messages_images%2FGroup-icon.png?alt=media&token=3ef7955e-783f-4a71-9224-bba311438fc3";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        ref = FirebaseDatabase.getInstance().getReference();
        tLayout = findViewById(R.id.main_tabs);
        vPager = findViewById(R.id.main_tabs_pager);

        tabsPagerAdapter=new TabsPagerAdapter(getSupportFragmentManager());
        vPager.setAdapter(tabsPagerAdapter);
        tLayout.setupWithViewPager(vPager);

        sp = new SharedPreferencesConfig(this);

        mAuth = FirebaseAuth.getInstance();

        validation = new Validation();
        config = new GroupsConfig();
        //db = new LocalDatabaseHelper(this);
        //db.insertData("123","v1","HelloWorld");
        //db.deleteData("123");
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateUserState("online");
    }

    @Override
    protected void onStop() {
        super.onStop();

        updateUserState("offline");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        updateUserState("offline");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {  //for adding menu on the activity

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main2_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {  //for doing something when an item is clicked

        int id=item.getItemId();

        if(id==R.id.menu_account)
        {
            startActivity(new Intent(this,SetProfile.class));
        }
        if(id==R.id.menu_settings)
        {
            startActivity(new Intent(this,Settings.class));
        }
        if(id==R.id.menu_find_freinds)
        {
            startActivity(new Intent(this,AddContact.class));
        }
        if(id==R.id.menu_create_group)
        {
            requestNewGroup();
        }
        if(id==R.id.menu_logOut)
        {
            updateUserState("offline");
            sp.writeLoginStatus(false);

            Intent intent = new Intent(this, Authentication.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();

            /*Intent i = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);*/
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestNewGroup() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Group Name");

        final EditText group = new EditText(this);
        group.setHint("eg : School Friends");
        builder.setView(group);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String groupName = group.getText().toString();
                boolean validateGroupName = validation.validateGroupName(groupName);

                if(validateGroupName)
                {
                    if(TextUtils.isEmpty(groupName))
                    {
                        Toast.makeText(Main2Activity.this, "Please Enter A Group Name ....", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        createNewGroup(groupName);
                    }
                }
                else
                {
                    Toast.makeText(Main2Activity.this, "Please enter a valid group name", Toast.LENGTH_SHORT).show();
                }

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        
        builder.show();
    }

    private void createNewGroup(final String groupName) {


        long start = System.nanoTime();
        String groupId = config.createGroup(groupName);
        long elapsed = System.nanoTime() - start;
        ref.child("Time").child("groupCreation").setValue(elapsed);
        Intent intent = new Intent(Main2Activity.this,GroupMemberSelection.class);
        intent.putExtra("groupKey",groupId);
        startActivity(intent);
    }

    public void updateUserState(String state)
    {
        String saveCurrentTime, saveCurrentDate;

        Calendar calender = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd yyyy");
        saveCurrentDate = currentDate.format(calender.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calender.getTime());

        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", saveCurrentTime);
        onlineStateMap.put("date", saveCurrentDate);
        onlineStateMap.put("state", state);

        currentUserId = mAuth.getCurrentUser().getUid();

        ref.child("users").child(currentUserId).child("user-state")
                .updateChildren(onlineStateMap);
    }
}
