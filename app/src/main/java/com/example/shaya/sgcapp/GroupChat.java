package com.example.shaya.sgcapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shaya.sgcapp.adapters.MessageAdapter;
import com.example.shaya.sgcapp.domain.modelClasses.Messages;
import com.example.shaya.sgcapp.UI.GroupSettings;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChat extends AppCompatActivity {

    private EditText inputMessage;
    private ImageButton sendMessageButton;
    //private ImageButton sendImageButton;

    private TextView groupDisplayName, groupDisplayMembers;
    private CircleImageView groupDisplayImage;

    private String groupId;

    private String currentUserId;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef, rootRef;
    private StorageReference firebaseStorage;

    //RelativeLayout relativeLayout;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView groupChatMessagesList;

    private ProgressDialog loadingBar;

    private Security security;
    private String key;
    private String keyVal;
    private LocalDatabaseHelper db;
    private LocalDatabase database;
    private GroupsConfig config;

    //private ValueEventListener postListener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        getSupportActionBar().hide();

        Intent intent = getIntent();
        groupId = intent.getStringExtra("group_id");

        initializeControllers();

        getGroupData();
        getGroupKey();

        groupDisplayName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupInfo();
            }
        });
        groupDisplayMembers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupInfo();
            }
        });
        groupDisplayImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupInfo();
            }
        });

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendGroupMessage();
            }
        });

        /*sendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImage();
            }
        });*/

    }

    private void getGroupKey() {

        rootRef.child("group-users").child(groupId).child(currentUserId).child("keyChange").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    String keyChangeNotification = dataSnapshot.getValue().toString();

                    if(keyChangeNotification.equals("successful"))
                    {

                        rootRef.child("groups").child(groupId).child("Security").child("key").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists())
                                {
                                    String version = dataSnapshot.getValue().toString();
                                    int count = 0;

                                    Cursor res = db.getData(groupId,key);
                                    if(res.getCount() != 0)
                                    {
                                        //StringBuffer buffer = new StringBuffer();
                                        while (res.moveToNext()) {
                                            //buffer.append("Key Value : " + res.getString(3) + "\n");
                                            String grp = res.getString(1);
                                            if(grp.equals(groupId))
                                            {
                                                String ver = res.getString(2);
                                                if(ver.equals(version))
                                                {
                                                    count++;
                                                }
                                            }
                                        }
                                    }

                                    if(count == 0)
                                    {
                                        Cursor result = database.getData(groupId);
                                        if(result.getCount() != 0)
                                        {
                                            while(result.moveToNext())
                                            {
                                                String grp = result.getString(0);
                                                if(grp.equals(groupId))
                                                {
                                                    String s0 = result.getString(2);
                                                    String t0 = result.getString(3);

                                                    try
                                                    {
                                                        String newGroupKey = generateNewKey(s0,t0);
                                                        Toast.makeText(GroupChat.this, "Group Key Updated", Toast.LENGTH_SHORT).show();
                                                        db.insertData(groupId,version,newGroupKey);

                                                    }catch (Exception e)
                                                    {

                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //getGroupData();
        messagesList.clear();

    }

    @Override
    protected void onRestart() {
        super.onRestart();

        //getGroupData();
        messagesList.clear();
    }

    private void getGroupData() {

        security = new Security();

        rootRef.child("groups").child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                groupDisplayName.setText(dataSnapshot.child("Name").getValue().toString());
                groupDisplayMembers.setText("Members: " + dataSnapshot.child("Total_Members").getValue().toString());
                Picasso.get().load(dataSnapshot.child("Group_Pic").getValue().toString()).into(groupDisplayImage);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        rootRef.child("groups").child(groupId).child("Security").child("distribution").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists())
                {
                    String dist = dataSnapshot.getValue().toString();

                    if(dist.equals("unicast"))
                    {
                        rootRef.child("group-users").child(groupId).child(currentUserId).child("keyChange").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if(dataSnapshot.exists())
                                {
                                    String keyChangeNotification = dataSnapshot.getValue().toString();

                                    if(keyChangeNotification.equals("unsuccessful"))
                                    {
                                        rootRef.child("group-users").child(groupId).child(currentUserId).child("encKey").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(dataSnapshot.exists())
                                                {
                                                    String encKey = dataSnapshot.getValue().toString();
                                                    String decKey = "";
                                                    try
                                                    {
                                                        decKey = security.decrypt(encKey, currentUserId);

                                                    }catch (Exception e)
                                                    {

                                                    }

                                                    db.insertData(groupId, "0", decKey);
                                                    rootRef.child("group-users").child(groupId).child(currentUserId).child("encKey").removeValue();
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                        rootRef.child("group-users").child(groupId).child(currentUserId).child("encHash").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                if(dataSnapshot.exists())
                                                {
                                                    String encHash = dataSnapshot.getValue().toString();
                                                    String decHash = "";
                                                    try
                                                    {
                                                        decHash = security.decrypt(encHash, currentUserId);

                                                    }catch (Exception e)
                                                    {

                                                    }

                                                    Cursor res = database.getData(groupId);
                                                    int count = 0;
                                                    if(res.getCount() != 0)
                                                    {
                                                        while (res.moveToNext()) {
                                                            //buffer.append("Key Value : " + res.getString(3) + "\n");
                                                            String grp = res.getString(0);
                                                            if(grp.equals(groupId))
                                                            {
                                                                count++;
                                                            }
                                                        }
                                                    }

                                                    if(count == 0)
                                                    {
                                                        String keyV = "";

                                                        Cursor result = db.getData(groupId,key);
                                                        if(result.getCount() != 0)
                                                        {
                                                            //StringBuffer buffer = new StringBuffer();
                                                            while (result.moveToNext()) {
                                                                //buffer.append("Key Value : " + res.getString(3) + "\n");
                                                                String grp = result.getString(1);
                                                                if(grp.equals(groupId))
                                                                {
                                                                    String ver = result.getString(2);
                                                                    if(ver.equals("0"))
                                                                    {
                                                                        keyV = result.getString(3);
                                                                    }
                                                                }
                                                            }
                                                        }


                                                        database.insertData(groupId,"0",keyV,decHash);
                                                        rootRef.child("group-users").child(groupId).child(currentUserId).child("keyChange").setValue("successful");
                                                        //Toast.makeText(GroupChat.this, ""+keyV, Toast.LENGTH_LONG).show();
                                                    }
                                                    else
                                                    {
                                                        Cursor r = database.getData(groupId);
                                                        String s = "";

                                                        if(r.getCount() != 0)
                                                        {
                                                            //StringBuffer buffer = new StringBuffer();
                                                            while (r.moveToNext()) {
                                                                //buffer.append("Key Value : " + res.getString(3) + "\n");
                                                                String grp = r.getString(0);
                                                                if(grp.equals(groupId))
                                                                {
                                                                    s = r.getString(2);
                                                                }
                                                            }
                                                        }

                                                        final String finalS = s;
                                                        final String finalDecHash = decHash;
                                                        rootRef.child("groups").child(groupId).child("Security").child("key").addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                if(dataSnapshot.exists())
                                                                {
                                                                    String version = dataSnapshot.getValue().toString();

                                                                    try
                                                                    {
                                                                        String hashedS = applyHash(finalS);
                                                                        database.insertData(groupId,version,hashedS, finalDecHash);
                                                                        rootRef.child("group-users").child(groupId).child(currentUserId).child("keyChange").setValue("successful");

                                                                    }catch (Exception e)
                                                                    {

                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                    else if(dist.equals("updateGroup"))
                    {
                        rootRef.child("group-users").child(groupId).child(currentUserId).child("groupStatus").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists())
                                {
                                    String groupStatus = dataSnapshot.getValue().toString();

                                    if(groupStatus.equals("admin"))
                                    {
                                        ArrayList<String> list = new ArrayList<>();
                                        config.deleteGroupMembers(list,groupId,GroupChat.this);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        rootRef.child("group-users").child(groupId).orderByChild("keyChange").equalTo("unsuccessful").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists())
                {

                }
                else
                {
                    rootRef.child("groups").child(groupId).child("Security").child("distribution").setValue("successful");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private String applyHash(String s) throws Exception {

        String newS = s;

        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = newS.getBytes("UTF-8");
        digest.update(bytes, 0, bytes.length);
        byte[] key = digest.digest();

        StringBuffer hexString = new StringBuffer();
        for (int j=0; j<key.length; j++)
        {
            hexString.append(Integer.toHexString(0xFF & key[j]));
        }
        String hashed = hexString.toString();
        return hashed;
    }

    private String generateNewKey(String s, String t) throws Exception {

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length(); i++)
            sb.append((char)(s.charAt(i) ^ t.charAt(i % t.length())));
        String result = sb.toString();

        //String result = security.encrypt(s,t);
        return result;
    }

    @Override
    protected void onStart() {
        super.onStart();

        messagesList.clear();

        /*Cursor result = database.getData(groupId);
        if(result.getCount() != 0)
        {
            while(result.moveToNext())
            {
                String grp = result.getString(0);
                if(grp.equals(groupId))
                {
                    String s0 = result.getString(2);
                    String t0 = result.getString(3);
                    Toast.makeText(this, ""+s0, Toast.LENGTH_LONG).show();
                    //Toast.makeText(this, ""+t0, Toast.LENGTH_LONG).show();
                }
            }
        }*/

        rootRef.child("groups").child(groupId).child("Security").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists())
                {
                    key = dataSnapshot.child("key").getValue().toString();
                    keyVal = "";

                    Cursor res = db.getData(groupId,key);
                    if(res.getCount() != 0)
                    {
                        //StringBuffer buffer = new StringBuffer();
                        while (res.moveToNext()) {
                            //buffer.append("Key Value : " + res.getString(3) + "\n");
                            String grp = res.getString(1);
                            if(grp.equals(groupId))
                            {
                                String ver = res.getString(2);
                                if(ver.equals(key))
                                {
                                    keyVal = res.getString(3);
                                    //Toast.makeText(GroupChat.this, ""+keyVal, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        rootRef.child("group-messages").child(groupId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);
                messageAdapter.notifyDataSetChanged();
                //finish();
                //
                // startActivity(getIntent());
                groupChatMessagesList.smoothScrollToPosition(groupChatMessagesList.getAdapter().getItemCount());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void initializeControllers()
    {
        ActionBar actionBar = getSupportActionBar();
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        //relativeLayout = actionBarView.findViewById(R.id.custom_chat_bar_layout);
        actionBar.setCustomView(actionBarView);

        rootRef = FirebaseDatabase.getInstance().getReference();
        userRef = FirebaseDatabase.getInstance().getReference().child("users");
        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        firebaseStorage = FirebaseStorage.getInstance().getReference().child("group_messages_images").child(groupId);

        groupDisplayName = findViewById(R.id.custom_chat_bar_name);
        groupDisplayMembers = findViewById(R.id.custom_chat_bar_status);
        groupDisplayImage = findViewById(R.id.custom_chat_bar_image);
        inputMessage = findViewById(R.id.group_chat_input_messages);
        sendMessageButton = findViewById(R.id.group_chat_send_message_btn);
        //sendImageButton = findViewById(R.id.group_chat_send_image);

        groupChatMessagesList = findViewById(R.id.group_chat_messages_list);
        linearLayoutManager = new LinearLayoutManager(this);
        groupChatMessagesList.setLayoutManager(linearLayoutManager);

        loadingBar = new ProgressDialog(this);

        messageAdapter = new MessageAdapter(messagesList, groupId, this);
        groupChatMessagesList.setAdapter(messageAdapter);
        db = new LocalDatabaseHelper(this);
        security = new Security();
        database = new LocalDatabase(this);
        config = new GroupsConfig();
    }

    private void sendGroupMessage() {

        String sendingMsg = inputMessage.getText().toString();
        inputMessage.setText("");
        //security = new Security();
        String encryptedVal = "";
        try
        {
            encryptedVal = security.encrypt(sendingMsg, keyVal);

        }catch (Exception e)
        {
            //Toast.makeText(this, ""+e, Toast.LENGTH_SHORT).show();
        }

        if(!sendingMsg.isEmpty())
        {
            DatabaseReference userMessageKeyRef = rootRef.child("group-messages").child(groupId).push();

            String msgPushId = userMessageKeyRef.getKey();

            Map msgTextBody = new HashMap();
            msgTextBody.put("message", encryptedVal);
            msgTextBody.put("type", "text");
            msgTextBody.put("from", currentUserId);
            msgTextBody.put("msgKey", msgPushId);
            msgTextBody.put("keyVersion", key);

            String messageSenderRef = "group-messages/" + groupId;

            Map msgBodyDetails = new HashMap();
            msgBodyDetails.put(messageSenderRef + "/" + msgPushId, msgTextBody);

            rootRef.updateChildren(msgBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {

                    if(!task.isSuccessful())
                    {
                        Toast.makeText(GroupChat.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void groupInfo() {

        Intent intent = new Intent(this, GroupSettings.class);
        intent.putExtra("groupId",groupId);
        startActivity(intent);
        finish();
    }

    public void sendImage(View view)
    {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, 100);
    }

    public void sendAudio(View view)
    {
        Intent intent;
        intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        //intent.setType("audio/*");
        startActivityForResult(intent, 10);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100 && resultCode == RESULT_OK && data != null)
        {
            loadingBar.setTitle("Sending Image");
            loadingBar.setMessage("Please wait while your image is uploading");
            loadingBar.show();

            Uri encUri;

            security = new Security();
            final Uri imageUri = data.getData();
            File file = new File(getRealPathFromURI(imageUri));
            File encFile = new File(security.encryptFile(file,keyVal));
            encUri = Uri.fromFile(encFile);

            //Toast.makeText(this, ""+ imageUri.toString(), Toast.LENGTH_LONG).show();


            final String messageSenderRef = "group-messages/" + groupId;

            DatabaseReference userMessageKeyRef = rootRef.child("group-messages").child(groupId).push();

            final String msgPushId = userMessageKeyRef.getKey();

            final StorageReference filePath = firebaseStorage.child(msgPushId+".jpg");
            filePath.putFile(encUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful())
                    {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadUrl = uri.toString();

                                Map msgTextBody = new HashMap();
                                msgTextBody.put("message", downloadUrl);
                                msgTextBody.put("type", "image");
                                msgTextBody.put("from", currentUserId);
                                msgTextBody.put("msgKey", msgPushId);
                                msgTextBody.put("keyVersion", key);

                                Map msgBodyDetails = new HashMap();
                                msgBodyDetails.put(messageSenderRef + "/" + msgPushId, msgTextBody);

                                rootRef.updateChildren(msgBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {

                                        if(!task.isSuccessful())
                                        {
                                            Toast.makeText(GroupChat.this, "Error sending image", Toast.LENGTH_SHORT).show();
                                        }
                                        inputMessage.setText("");
                                        loadingBar.dismiss();
                                        finish();
                                        startActivity(getIntent());
                                    }
                                });
                            }
                        });
                    }
                    else
                    {
                        Toast.makeText(GroupChat.this, "Picture not sent. Please try again", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }

                }
            });


        }
        else if(requestCode == 10 && resultCode == RESULT_OK && data != null)
        {
            loadingBar.setTitle("Sending Audio");
            loadingBar.setMessage("Please wait while your file is uploading");
            loadingBar.show();

            Uri encUri;

            security = new Security();
            final Uri audioUri = data.getData();
            String path = getAudioPath(audioUri);
            File file = new File(path);
            File encFile = new File(security.encryptFile(file,keyVal));
            encUri = Uri.fromFile(encFile);

            //Toast.makeText(this, ""+encUri.toString(), Toast.LENGTH_SHORT).show();

            final String messageSenderRef = "group-messages/" + groupId;

            DatabaseReference userMessageKeyRef = rootRef.child("group-messages").child(groupId).push();

            final String msgPushId = userMessageKeyRef.getKey();

            final StorageReference filePath = firebaseStorage.child(msgPushId+".mp3");
            filePath.putFile(encUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful())
                    {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadUrl = uri.toString();

                                Map msgTextBody = new HashMap();
                                msgTextBody.put("message", downloadUrl);
                                msgTextBody.put("type", "audio");
                                msgTextBody.put("from", currentUserId);
                                msgTextBody.put("msgKey", msgPushId);
                                msgTextBody.put("keyVersion", key);

                                Map msgBodyDetails = new HashMap();
                                msgBodyDetails.put(messageSenderRef + "/" + msgPushId, msgTextBody);

                                rootRef.updateChildren(msgBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                    @Override
                                    public void onComplete(@NonNull Task task) {

                                        if(!task.isSuccessful())
                                        {
                                            Toast.makeText(GroupChat.this, "Error sending file", Toast.LENGTH_SHORT).show();
                                        }
                                        inputMessage.setText("");
                                        loadingBar.dismiss();
                                        finish();
                                        startActivity(getIntent());
                                    }
                                });
                            }
                        });
                    }
                    else
                    {
                        Toast.makeText(GroupChat.this, "File not sent. Please try again", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }

                }
            });

        }
    }

    private String getRealPathFromURI(Uri contentUri)
    {
        String[] proj = { MediaStore.Audio.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private String getAudioPath(Uri uri) {
        String[] data = {MediaStore.Audio.Media.DATA};
        CursorLoader loader = new CursorLoader(getApplicationContext(), uri, data, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
}
