package com.example.content;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.content.ContactAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ_CONTACTS = 1;
    private static final int REQUEST_WRITE_CONTACTS = 2;
    private static final int REQUEST_CALL_PHONE = 3;

    private Button btnAddContact, btnReadContacts, btnCallContact;
    private RecyclerView recyclerViewContacts;
    private TextView tvNoContacts;
    private ContactAdapter contactAdapter;
    private List<Contact> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnAddContact = findViewById(R.id.btnAddContact);
        btnReadContacts = findViewById(R.id.btnReadContacts);
        btnCallContact = findViewById(R.id.btnCallContact);
        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        tvNoContacts = findViewById(R.id.tvNoContacts);

        btnAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    addContact();
                } else {
                    requestPermissions(new String[]{Manifest.permission.WRITE_CONTACTS}, REQUEST_WRITE_CONTACTS);
                }
            }
        });

        btnReadContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    readContacts();
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
                }
            }
        });

        btnCallContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    callContact();
                } else {
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
                }
            }
        });

        contactList = new ArrayList<>();
        contactAdapter = new ContactAdapter(contactList);
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewContacts.setAdapter(contactAdapter);
    }

    private void addContact() {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "张三");
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, "123456789");

        ContentResolver contentResolver = getContentResolver();
        Uri rawContactsUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values);
        if (rawContactsUri != null) {
            long rawContactId = ContentUris.parseId(rawContactsUri);
            Toast.makeText(MainActivity.this, "联系人添加成功", Toast.LENGTH_SHORT).show();
            readContacts();
        }

    }
    @SuppressLint("Range")
    private void readContacts() {
        // 获取ContentResolver对象，用于访问系统数据
        ContentResolver contentResolver = getContentResolver();

        // 查询联系人数据
        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.getCount() > 0) {
            contactList.clear();
            Cursor phoneCursor = null;
            while (cursor.moveToNext()) {
                // 获取联系人ID和姓名
                @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                // 查询电话号码
                phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id},
                        null
                );

                String phoneNumber = "";
                if (phoneCursor != null && phoneCursor.moveToNext()) {
                    phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }

                contactList.add(new Contact(id, name, phoneNumber));
            }
            cursor.close();
            phoneCursor.close();
            contactAdapter.notifyDataSetChanged();
            recyclerViewContacts.setVisibility(View.VISIBLE);
            tvNoContacts.setVisibility(View.GONE);
        } else {
            contactList.clear();
            contactAdapter.notifyDataSetChanged();
            recyclerViewContacts.setVisibility(View.GONE);
            tvNoContacts.setVisibility(View.VISIBLE);
        }
    }

    private void callContact() {
        if (contactList.isEmpty()) {
            Toast.makeText(this, "请先读取联系人列表", Toast.LENGTH_SHORT).show();
            return;
        }

        Contact contact = contactList.get(0);
        String phoneNumber = contact.getPhoneNumber();

        if (!phoneNumber.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_CALL);
            Uri uri = Uri.parse("tel:" + phoneNumber);
            intent.setData(uri);
            startActivity(intent);
        } else {
            Toast.makeText(this, "该联系人没有电话号码", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_READ_CONTACTS:
                    readContacts();
                    break;
                case REQUEST_WRITE_CONTACTS:
                    addContact();
                    break;
                case REQUEST_CALL_PHONE:
                    callContact();
                    break;
            }
        }
    }
}
