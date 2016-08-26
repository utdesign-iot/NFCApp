package com.example.torin.nfcapp;


import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import com.example.torin.nfcapp.R;

public class MainActivity extends AppCompatActivity
{
    private NfcAdapter nfcAdapter;
    public final static String URL = "http://ecs.utdallas.edu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if nfc adapter is null, then it means that the device does not have an nfc reader.
        if(nfcAdapter != null && nfcAdapter.isEnabled())
        {
            Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            IntentFilter[] intentFilters = new IntentFilter[]{};
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //disables nfc on pause if device supports nfc
        if(nfcAdapter != null && nfcAdapter.isEnabled())
            nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //if new intent is nfc
        if(intent.hasExtra(NfcAdapter.EXTRA_TAG))
        {
            Toast.makeText(this, "NfcIntent", Toast.LENGTH_SHORT).show();
            //get ndef messages
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            // read messages if there are any on tag
            if(parcelables != null && parcelables.length > 0)
                readTextFromMessage((NdefMessage) parcelables[0]);
            else
                Toast.makeText(this, "No NDEF messages found", Toast.LENGTH_SHORT).show();
        }
        //else handle as a normal intent
        else
            setIntent(intent);
    }

    //This function extracts urls from ndef messages and sends them to the browser activity
    private void readTextFromMessage (NdefMessage ndefMessage)
    {
        NdefRecord[] ndefRecords = ndefMessage.getRecords();
        if(ndefRecords != null && ndefRecords.length > 0)
        {
            //first record has url
            NdefRecord ndefRecord = ndefRecords[0];
            //get text from first record
            String tagContent = getTextFromNdefRecord(ndefRecord);
            //create intent
            Intent intent = new Intent(this, BrowserActivity.class);
            intent.putExtra(URL, tagContent);
            //send to browser activity
            startActivity(intent);
        }
        else
            Toast.makeText(this, "No NDEF records found", Toast.LENGTH_SHORT).show();
    }

    //This function extracts the text from an ndef record
    public String getTextFromNdefRecord(NdefRecord ndefRecord)
    {
        String tagContent = null;
        try
        {
            //decode ndef record based on ndef standards
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            //store url in new string
            tagContent = new String(payload, languageSize + 1, payload.length - languageSize - 1, textEncoding);
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        //return url
        return tagContent;
    }
}
