package com.example.mugeshm.oneremote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zokama.androlirc.Lirc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
   private final static String LIRCD_CONF_FILE = "/data/data/com.example.mugeshm.oneremote/lircd.conf";
//private final static String LIRCD_CONF_FILE = "/sdcard0/lircd.conf";

    // global variables
    TextView tv;
    Lirc lirc;
    ArrayAdapter<String> deviceList;
    ArrayAdapter<String> commandList;
    AudioTrack ir;
    int minBufSize;
    String address;
    ArrayList<String> str = new ArrayList<String>();

    // Check if the first level of the directory structure is the one showing
    private Boolean firstLvl = true;

    private static final String TAG = "F_PATH";

    private Item[] fileList;
    private File path = new File(Environment.getExternalStorageDirectory() + "");
    private String chosenFile;
    private static final int DIALOG_LOAD_FILE = 1000;

    ListAdapter adapter;

    private static final int PICKFILE_RESULT_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        //ll.setKeepScreenOn(true);
        setContentView(ll);

        ScrollView sv = new ScrollView(this);
        tv = new TextView(this);
        //tv.setBackgroundColor();
        sv.addView(tv);
        lirc = new Lirc();
       // playLock = false;

        // Initialize adapter for device spinner
        deviceList = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        deviceList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Initialize device spinner
        final Spinner spinDevice = new Spinner(this);
        spinDevice.setPrompt("Select a device");

        spinDevice.setAdapter(deviceList);

        // Command adapter
        commandList = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
        commandList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Parse configuration file and update device adapter
        parse(LIRCD_CONF_FILE);

        spinDevice.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                updateCommandList(spinDevice.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        ll.addView(spinDevice);

        final Spinner spinCommand = new Spinner(this);
        spinCommand.setPrompt("Select a command");
        spinCommand.setAdapter(commandList);
        ll.addView(spinCommand);

        Button btn = new Button(this);
        btn.setText("Send power IR cmd");
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (spinDevice.getSelectedItem()==null || spinCommand.getSelectedItem() == null) {
                    Toast.makeText(getApplicationContext(), "Please select a device and a command", Toast.LENGTH_SHORT).show();
                    return;
                }

                String device = spinDevice.getSelectedItem().toString();
                String cmd = spinCommand.getSelectedItem().toString();
                sendSignal(device, cmd);
            }});

        ll.addView(btn);
        ll.addView(sv);

        // Prepare audio buffer
        minBufSize = AudioTrack.getMinBufferSize(48000,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_8BIT);

        ir = new AudioTrack(AudioManager.STREAM_MUSIC, 48000,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_8BIT,
                minBufSize, AudioTrack.MODE_STREAM);

        ir.play();
    }
    public void loadFileList() {
        try {
            path.mkdirs();
        } catch (SecurityException e) {
            Log.e(TAG, "unable to write on the sd card ");
        }

        // Checks whether path exists
        if (path.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    // Filters based on whether the file is hidden or not
                    return (sel.isFile() || sel.isDirectory())
                            && !sel.isHidden();

                }
            };

            String[] fList = path.list(filter);
            fileList = new Item[fList.length];
            for (int i = 0; i < fList.length; i++) {
                fileList[i] = new Item(fList[i], R.drawable.file_icon);

                // Convert into file path
                File sel = new File(path, fList[i]);

                // Set drawables
                if (sel.isDirectory()) {
                    fileList[i].icon = R.drawable.directory_icon;
                    Log.d("DIRECTORY", fileList[i].file);
                } else {
                    Log.d("FILE", fileList[i].file);
                }
            }

            if (!firstLvl) {
                Item temp[] = new Item[fileList.length + 1];
                for (int i = 0; i < fileList.length; i++) {
                    temp[i + 1] = fileList[i];
                }
                temp[0] = new Item("Up", R.drawable.directory_up);
                fileList = temp;
            }
        } else {
            Log.e(TAG, "path does not exist");
        }

        adapter = new ArrayAdapter<Item>(this,
                android.R.layout.select_dialog_item, android.R.id.text1,
                fileList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // creates view
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view
                        .findViewById(android.R.id.text1);

                // put the image on the text view
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        fileList[position].icon, 0, 0, 0);

                // add margin between image and text (support various screen
                // densities)
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp5);

                return view;
            }
        };

    }

    private class Item {
        public String file;
        public int icon;

        public Item(String file, Integer icon) {
            this.file = file;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return file;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (fileList == null) {
            Log.e(TAG, "No files loaded");
            dialog = builder.create();
            return dialog;
        }

        switch (id) {
            case DIALOG_LOAD_FILE:
                builder.setTitle("Select a file to parse");
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chosenFile = fileList[which].file;
                        File sel = new File(path + "/" + chosenFile);
                        if (sel.isDirectory()) {
                            firstLvl = false;

                            // Adds chosen directory to list
                            str.add(chosenFile);
                            fileList = null;
                            path = new File(sel + "");

                            loadFileList();

                            removeDialog(DIALOG_LOAD_FILE);
                            showDialog(DIALOG_LOAD_FILE);
                            Log.d(TAG, path.getAbsolutePath());

                        }

                        // Checks if 'up' was clicked
                        else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {

                            // present directory removed from list
                            String s = str.remove(str.size() - 1);

                            // path modified to exclude present directory
                            path = new File(path.toString().substring(0,
                                    path.toString().lastIndexOf(s)));
                            fileList = null;

                            // if there are no more directories in the list, then
                            // its the first level
                            if (str.isEmpty()) {
                                firstLvl = true;
                            }
                            loadFileList();

                            removeDialog(DIALOG_LOAD_FILE);
                            showDialog(DIALOG_LOAD_FILE);
                            Log.d(TAG, path.getAbsolutePath());

                        }
                        // File picked
                        else {
                            // Perform action with file picked
                            address  = sel.getAbsolutePath();

                            parse(address);
                        }

                    }
                });
                break;
        }
        dialog = builder.show();
        return dialog;
    }
    public String selectFile() {

        loadFileList();
        showDialog(DIALOG_LOAD_FILE);
        Log.d(TAG, path.getAbsolutePath());
        return null;
    }

    public boolean parse (String config_file) {

        java.io.File file = new java.io.File(config_file);
        if (!file.exists()) {
            if (!config_file.equals(LIRCD_CONF_FILE)) {
                try {
                    Toast.makeText(getApplicationContext(), "The Selected file doesn't exist", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    tv.append("The Selected file doesn't exist " + e.getMessage());
                }
            }
            else {
             try{   Toast.makeText(getApplicationContext(), "Configuartion file missing", Toast.LENGTH_SHORT).show();}
             catch (Exception e) {
                 tv.append("Configuartion file missing " + e.getMessage());
             }
            } selectFile();
            return false;
        }

        if (lirc.parse(config_file) == 0) {
           try {
               Toast.makeText(getApplicationContext(), "Couldn't parse the selected file", Toast.LENGTH_SHORT).show();
               selectFile();
               return false;
           } catch (Exception e){
               tv.append("Couldn't parse the selected file "+ e.getMessage());
           }
        }

        // Save the file since it has been parsed successfully
        if (!config_file.equals(LIRCD_CONF_FILE)) {
            try {
                FileInputStream in = new FileInputStream(config_file);
                FileOutputStream out = new FileOutputStream(LIRCD_CONF_FILE);
                byte[] buf = new byte[1024];
                int i = 0;
                while ((i = in.read(buf)) != -1) {
                    out.write(buf, 0, i);
                }
                in.close();
                out.close();
            } catch(Exception e) {
                tv.append("Probleme saving configuration file: "+ e.getMessage());
            }
        }

        updateDeviceList();
        return true;
    }



    public void updateDeviceList(){
        String [] str = lirc.getDeviceList();

        if (str == null){
            Toast.makeText(getApplicationContext(), "Invalid, empty or missing config file", Toast.LENGTH_SHORT).show();
            selectFile();
            return;
        }

        deviceList.clear();
        for (int i=0; i<str.length; i++){
            Log.e("ANDROLIRC", String.valueOf(i + 1) + "/" + String.valueOf(str.length) + ": " + str[i]);
            deviceList.add(str[i]);
        }

        Log.e("ANDROLIRC", "Device list successfuly updated. Number of devices: "+String.valueOf(str.length));
        updateCommandList(str[0]);
    }



    public void updateCommandList(String device){
        String [] str = lirc.getCommandList(device);

        if (str == null){
            Toast.makeText(getApplicationContext(), "No command found for the selected device", Toast.LENGTH_SHORT).show();
            return;
        }
        commandList.clear();

        for (int i=0; i<str.length; i++)
            commandList.add(str[i]);

        Log.e("ANDROLIRC", "Command list successfuly updated. Number of detected commands: "+String.valueOf(str.length));
    }



    void sendSignal(String device, String cmd) {

        byte buffer[] = lirc.getIrBuffer(device, cmd, minBufSize+1024);

        if (buffer == null) {
            tv.append("\nError retreiving buffer\n");
            return;
        }

        ir.setStereoVolume(1, 1);
        int res = ir.write(buffer, 0, buffer.length);

        Log.e("BUFFER", "written/buf_size/min_buf_size: "+res+"/"+buffer.length+"/"+minBufSize);
        tv.append(device + ": " +cmd + " command sent\n");
    }



    void deleteConfigFile() {
        java.io.File file = new java.io.File(LIRCD_CONF_FILE);
        if (!file.exists())
            Toast.makeText(getApplicationContext(), "Configuartion file missing\n" +
                    "No file to delete", Toast.LENGTH_SHORT).show();
        else
        if (file.delete()){
            Toast.makeText(getApplicationContext(), "File deleted successfully", Toast.LENGTH_SHORT).show();
            deviceList.clear();
            commandList.clear();
            selectFile();
        }
        else
            Toast.makeText(getApplicationContext(), "Couldn't delete the file", Toast.LENGTH_SHORT).show();
    }



    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Parse file").setIcon(android.R.drawable.ic_menu_upload);
        menu.add(0, 1, 0, "Clear conf").setIcon(android.R.drawable.ic_menu_delete);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                selectFile();
                break;
            case 1:
                deleteConfigFile();
                break;
        }
        return false;
    }
}
