package com.example.shaya.sgcapp;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.example.shaya.sgcapp.domain.Validation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Random;

public class GroupsConfig {

    private String defaultGroupPicUrl = "https://firebasestorage.googleapis.com/v0/b/sgcapp-8dcbb.appspot.com/o/group_messages_images%2FGroup-icon.png?alt=media&token=3ef7955e-783f-4a71-9224-bba311438fc3";
    private DatabaseReference ref;
    private FirebaseAuth mAuth;
    private Validation validation;
    private DatabaseReference groupRef;
    private DatabaseReference groupUserRef;
    private DatabaseReference userRef;
    //private String GK = "";
    private StorageReference storageRef;
    private LocalDatabaseHelper db;
    private Security enc;
    private ArrayList<String> members;
    private LocalDatabase database;

    public GroupsConfig() {
    }

    public String createGroup(String groupName)
    {
        mAuth = FirebaseAuth.getInstance();
        ref = FirebaseDatabase.getInstance().getReference();
        validation = new Validation();
        final String user = mAuth.getCurrentUser().getUid();
        DatabaseReference groupIdKeyRef = ref.child("groups").push();
        String groupId = groupIdKeyRef.getKey();
        boolean validateGroupName = validation.validateGroupName(groupName);

        if(validateGroupName)
        {
            groupIdKeyRef = ref.child("groups").push();
            groupId = groupIdKeyRef.getKey();

            final String finalGroupKey = groupId;
            ref.child("groups").child(groupId).child("Name").setValue(groupName).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful())
                    {
                        //Toast.makeText(Main2Activity.this, groupKey+" is created Successfully", Toast.LENGTH_SHORT).show();
                        ref.child("group-users").child(finalGroupKey).child(user).child("groupStatus").setValue("admin")
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        ref.child("users").child(user).child("user-groups").child(finalGroupKey).setValue("true");
                                        ref.child("groups").child(finalGroupKey).child("Total_Members").setValue(1);
                                        ref.child("groups").child(finalGroupKey).child("Security").child("Algo").setValue("AES");
                                        ref.child("groups").child(finalGroupKey).child("Security").child("key").setValue("");
                                        ref.child("groups").child(finalGroupKey).child("Security").child("distribution").setValue("unicast");
                                        ref.child("groups").child(finalGroupKey).child("Group_Pic").setValue(defaultGroupPicUrl);

                                    }
                                });
                    }
                }
            });
        }
        return groupId;
    }

    public String generateInvertedHashChain(String groupId) throws Exception {

        ref = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        String currentUser = mAuth.getCurrentUser().getUid();

        Random rand = new Random();
        String randomDigit = String.valueOf(rand.nextLong());
        ref.child("group-users").child(groupId).child(currentUser).child("tSeed").setValue(randomDigit);

        String hashValue = randomDigit;

        for(int i = 0; i<= 10; i++)
        {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = hashValue.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            byte[] key = digest.digest();

            StringBuffer hexString = new StringBuffer();
            for (int j=0; j<key.length; j++)
            {
                hexString.append(Integer.toHexString(0xFF & key[j]));
            }
            hashValue = hexString.toString();
        }
        return hashValue;
    }

    public String generateIthHash(int index, String seed)throws Exception
    {
        String hashValue = seed;
        index++;
        int count = 10-index;

        for(int i = 0; i< count; i++)
        {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = hashValue.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            byte[] key = digest.digest();

            StringBuffer hexString = new StringBuffer();
            for (int j=0; j<key.length; j++)
            {
                hexString.append(Integer.toHexString(0xFF & key[j]));
            }
            hashValue = hexString.toString();
        }
        return hashValue;

    }

    public String generateNextHash(String s) throws Exception
    {
        String hashValue = s;

        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = hashValue.getBytes("UTF-8");
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

    public void addGroupMembers(final ArrayList<String> members, final String groupId, String status, final Context context)
    {
        if(status.equals("new"))
        {
            groupRef = FirebaseDatabase.getInstance().getReference().child("group-users");
            ref = FirebaseDatabase.getInstance().getReference();

            for(int i=0;i<members.size();i++)
            {
                groupRef.child(groupId).child(members.get(i)).child("groupStatus").setValue("member");
                groupRef.child(groupId).child(members.get(i)).child("keyChange").setValue("unsuccessful");
                ref.child("users").child(members.get(i)).child("user-groups").child(groupId).setValue("true");

            }
            ref.child("groups").child(groupId).child("Total_Members").setValue(members.size()+1);
        }
        else
        {
            groupRef = FirebaseDatabase.getInstance().getReference().child("group-users");
            ref = FirebaseDatabase.getInstance().getReference();
            enc = new Security();
            db = new LocalDatabaseHelper(context);
            database = new LocalDatabase(context);
            mAuth = FirebaseAuth.getInstance();
            final String currentUserId = mAuth.getCurrentUser().getUid();

            addGroupMembers(members,groupId,"new",context);

            ref.child("groups").child(groupId).child("Security").child("key").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists())
                    {
                        final int version = Integer.parseInt(dataSnapshot.getValue().toString());

                        ref.child("group-users").child(groupId).child(currentUserId).child("tSeed").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists())
                                {
                                    String tSeed = dataSnapshot.getValue().toString();

                                    String newT0 = "";

                                    try {
                                        newT0 = generateIthHash(version, tSeed);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    final String finalNewT = newT0;
                                    updateAdminSettings(groupId,version,newT0,context,members,"add");

                                    ref.child("group-users").child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists())
                                            {
                                                for(DataSnapshot d: dataSnapshot.getChildren())
                                                {
                                                    final String member = d.getKey();

                                                    ref.child("group-users").child(groupId).child(member).child("groupStatus").addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                            if(dataSnapshot.exists())
                                                            {
                                                                String groupStatus = dataSnapshot.getValue().toString();

                                                                if(groupStatus.equals("member"))
                                                                {
                                                                    ref.child("group-users").child(groupId).child(member).child("keyChange").setValue("unsuccessful");

                                                                    try
                                                                    {
                                                                        String encHash = enc.encrypt(finalNewT, member);
                                                                        ref.child("group-users").child(groupId).child(member).child("encHash").setValue(encHash);

                                                                    }catch (Exception e)
                                                                    {

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
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private void updateAdminSettings(String groupId, int version, String finalNewT, Context context, ArrayList<String> members, String status)
    {

        if(status.equals("add"))
        {
            database = new LocalDatabase(context);
            ref = FirebaseDatabase.getInstance().getReference();
            enc = new Security();

            String s = "";
            Cursor result = database.getData(groupId);
            if(result.getCount() != 0)
            {
                while(result.moveToNext())
                {
                    String grp = result.getString(0);
                    if(grp.equals(groupId))
                    {
                        s = result.getString(2);
                    }
                }

                if(!s.equals(""))
                {
                    String newS = "";
                    try {
                        newS = generateNextHash(s);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    for(int i =0; i < members.size(); i++)
                    {
                        try
                        {
                            String encKey = enc.encrypt(newS,members.get(i));
                            ref.child("group-users").child(groupId).child(members.get(i)).child("encKey").setValue(encKey);

                        }catch (Exception e)
                        {

                        }
                    }

                    database.insertData(groupId, String.valueOf(version), newS, finalNewT);
                    try
                    {
                        String newGroupKey = generateNewKey(newS, finalNewT);
                        updateGroup(groupId,newGroupKey,context);
                        ref.child("groups").child(groupId).child("Security").child("distribution").setValue("unicast");

                    }catch (Exception e)
                    {

                    }
                }
                else
                {
                    ref.child("groups").child(groupId).child("Security").child("distribution").setValue("not working");
                }
            }
        }
        else
        {

            database = new LocalDatabase(context);
            ref = FirebaseDatabase.getInstance().getReference();
            enc = new Security();

            String s = "";
            Cursor result = database.getData(groupId);
            if(result.getCount() != 0)
            {
                while(result.moveToNext())
                {
                    String grp = result.getString(0);
                    if(grp.equals(groupId))
                    {
                        s = result.getString(2);
                    }
                }

                if(!s.equals(""))
                {
                    String newS = "";
                    try {
                        newS = generateNextHash(s);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    database.insertData(groupId, String.valueOf(version), newS, finalNewT);
                    try
                    {
                        String newGroupKey = generateNewKey(newS, finalNewT);
                        updateGroup(groupId,newGroupKey,context);
                        ref.child("groups").child(groupId).child("Security").child("distribution").setValue("unicast");

                    }catch (Exception e)
                    {

                    }
                }
                else
                {
                    ref.child("groups").child(groupId).child("Security").child("distribution").setValue("not working");
                }
            }
        }
    }

    public void setKey(String gK, String groupId, Context context, ArrayList<String> members) throws Exception {
        db = new LocalDatabaseHelper(context);
        ref = FirebaseDatabase.getInstance().getReference();
        enc = new Security();
        database = new LocalDatabase(context);

        String t0 = generateInvertedHashChain(groupId);

        ref.child("groups").child(groupId).child("Security").child("keyVersions").child("0").setValue(gK);
        ref.child("groups").child(groupId).child("Security").child("key").setValue("0");
        db.insertData(groupId,"0",gK);

        for(int i=0;i<members.size();i++)
        {
            String encKey = enc.encrypt(gK,members.get(i));
            ref.child("group-users").child(groupId).child(members.get(i)).child("encKey").setValue(encKey);
            String encHash = enc.encrypt(t0,members.get(i));
            ref.child("group-users").child(groupId).child(members.get(i)).child("encHash").setValue(encHash);
        }
        database.insertData(groupId,"0", gK, t0);
    }

    public void updateGroup(final String groupId, final String groupKey, Context context)
    {
        db = new LocalDatabaseHelper(context);
        ref = FirebaseDatabase.getInstance().getReference();

        ref.child("group-users").child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long c = dataSnapshot.getChildrenCount();
                ref.child("groups").child(groupId).child("Total_Members").setValue(c);


                ref.child("groups").child(groupId).child("Security").child("keyVersions").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if(dataSnapshot.exists())
                        {
                            int count = (int) dataSnapshot.getChildrenCount();
                            ref.child("groups").child(groupId).child("Security").child("keyVersions").child(String.valueOf(count)).setValue(groupKey);
                            ref.child("groups").child(groupId).child("Security").child("key").setValue(String.valueOf(count));
                            db.insertData(groupId,String.valueOf(count),groupKey);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void deleteGroupMembers(final ArrayList<String> deleteMembers, final String groupId, final Context context)
    {
        enc = new Security();
        ref = FirebaseDatabase.getInstance().getReference();
        members = new ArrayList<>();
        db = new LocalDatabaseHelper(context);
        database = new LocalDatabase(context);
        mAuth = FirebaseAuth.getInstance();
        final String currentUserId = mAuth.getCurrentUser().getUid();

        for (int i=0;i<deleteMembers.size();i++)
        {
            ref.child("group-users").child(groupId).child(deleteMembers.get(i)).removeValue();
            ref.child("users").child(deleteMembers.get(i)).child("user-groups").child(groupId).removeValue();
        }

        ref.child("groups").child(groupId).child("Security").child("key").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    final int version = Integer.parseInt(dataSnapshot.getValue().toString());

                    ref.child("group-users").child(groupId).child(currentUserId).child("tSeed").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists())
                            {
                                String tSeed = dataSnapshot.getValue().toString();

                                String newT0 = "";

                                try {
                                    newT0 = generateIthHash(version, tSeed);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                final String finalNewT = newT0;
                                updateAdminSettings(groupId,version,newT0,context,members,"delete");

                                ref.child("group-users").child(groupId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists())
                                        {
                                            for(DataSnapshot d: dataSnapshot.getChildren())
                                            {
                                                final String member = d.getKey();

                                                ref.child("group-users").child(groupId).child(member).child("groupStatus").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if(dataSnapshot.exists())
                                                        {
                                                            String groupStatus = dataSnapshot.getValue().toString();

                                                            if(groupStatus.equals("member"))
                                                            {
                                                                ref.child("group-users").child(groupId).child(member).child("keyChange").setValue("unsuccessful");

                                                                try
                                                                {
                                                                    String encHash = enc.encrypt(finalNewT, member);
                                                                    ref.child("group-users").child(groupId).child(member).child("encHash").setValue(encHash);

                                                                }catch (Exception e)
                                                                {

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
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    public void leaveGroup(final String groupId, final String currentUserId, Context context)
    {
        ref = FirebaseDatabase.getInstance().getReference();
        groupUserRef = FirebaseDatabase.getInstance().getReference().child("group-users");
        userRef = FirebaseDatabase.getInstance().getReference().child("users");
        database = new LocalDatabase(context);

        groupUserRef.child(groupId).child(currentUserId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if(task.isSuccessful())
                {
                    userRef.child(currentUserId).child("user-groups").child(groupId).removeValue()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if(task.isSuccessful())
                                    {
                                        ref.child("groups").child(groupId).child("Security").child("distribution").setValue("updateGroup");
                                        //database.deleteData(groupId);
                                    }
                                }
                            });
                }
            }
        });
    }

    public boolean changeGroupName(String groupName, String groupId)
    {
        groupRef = FirebaseDatabase.getInstance().getReference().child("groups");
        validation = new Validation();
        boolean validateGroupName = validation.validateGroupName(groupName);

        if(validateGroupName)
        {
            groupRef.child(groupId).child("Name").setValue(groupName);
            return true;
        }
        else
            return false;
    }

    public String changeGroupImage(final String groupId, Uri imageUri)
    {
        storageRef = FirebaseStorage.getInstance().getReference().child("group_profile_images");
        final String[] url = new String[1];

        final StorageReference filePath = storageRef.child(groupId+".jpg");
        filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                if(task.isSuccessful())
                {
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {

                            final String downloadUrl = uri.toString();
                            url[0] = downloadUrl;
                            groupRef.child(groupId).child("Group_Pic").setValue(downloadUrl);
                        }
                    });
                }
            }
        });
        return url[0];
    }

    public void deleteGroup(final String groupId, Context context)
    {
        ref = FirebaseDatabase.getInstance().getReference();
        db = new LocalDatabaseHelper(context);

        ref.child("group-users").child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists())
                {
                    for(DataSnapshot d : dataSnapshot.getChildren())
                    {
                        String user = d.getKey();

                        ref.child("users").child(user).child("user-groups").child(groupId).removeValue();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        ref.child("group-messages").child(groupId).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful())
                        {
                            ref.child("group-users").child(groupId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if(task.isSuccessful())
                                    {
                                        ref.child("groups").child(groupId).removeValue();
                                        //db.deleteData(groupId);

                                    }
                                }
                            });
                        }
                    }
                });

    }

    public void changeEncryptionKey(final String groupId, final String algo, final String encryptionKey, Context context)
    {
        db = new LocalDatabaseHelper(context);
        ref = FirebaseDatabase.getInstance().getReference();

        /*ref.child("groups").child(groupId).child("Security").child("Algo").setValue(algo);
        ref.child("groups").child(groupId).child("Security").child("keyVersions").removeValue();
        ref.child("group-messages").child(groupId).removeValue();

        ref.child("groups").child(groupId).child("Security").child("keyVersions").child("v").setValue(encryptionKey);
        ref.child("groups").child(groupId).child("Security").child("key").setValue("v");*/

        ref.child("groups").child(groupId).child("Security").child("keyVersions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists())
                {
                    int count = (int) dataSnapshot.getChildrenCount();
                    ref.child("groups").child(groupId).child("Security").child("Algo").setValue(algo);
                    ref.child("groups").child(groupId).child("Security").child("keyVersions").child("v"+count).setValue(encryptionKey);
                    ref.child("groups").child(groupId).child("Security").child("key").setValue("v"+count);
                    db.insertData(groupId,"v"+count,encryptionKey);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //db.deleteData(groupId);
        //db.insertData(groupId,"v",encryptionKey);
    }

    private String generateNewKey(String s, String t) throws Exception {

        enc = new Security();

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < s.length(); i++)
            sb.append((char)(s.charAt(i) ^ t.charAt(i % t.length())));
        String result = sb.toString();

        //String result = enc.encrypt(s,t);

        return result;
    }
}
