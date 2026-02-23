package com.example.mytranslator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.preference.PreferenceManager;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private TextInputEditText textInputEditText;
    SQLiteOpenHelper wordDatabaseHelper;
    SharedPreferences sharedPreferences;
    int limit = 3;
    int difficulty;
    int vocabulary;
    boolean sick_mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        textInputEditText = findViewById(R.id.original_text);
        wordDatabaseHelper = new WordDatabaseHelper(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        wordDatabaseHelper.close();
    }

    //Create the toolbar
    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //When the translate button is clicked, initiate the asynchronous task
    public void onClickTranslate(View view) {
        // fetch preference parameters
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        difficulty = Integer.parseInt(sharedPreferences.getString("difficulty", "1"));
        vocabulary = Integer.parseInt(sharedPreferences.getString("vocabulary", "5000"));
        sick_mode = sharedPreferences.getBoolean("sick_mode", false);
        //Log.d("TAG", "diff：" + difficulty + " voca: " + vocabulary + " sick_mode: " + sick_mode);
        new TranslateTask().execute();

    }
    @Override
    // which activity to jump to when toolbar icon is clicked
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.preference) {//Code to run when item is clicked
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    // copy the translated text
    public void onClickCopy(View view) {
        TextView text_output = findViewById(R.id.target_text);
        String text = text_output.getText().toString();
        ClipboardManager cmb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setPrimaryClip(ClipData.newPlainText(null, text));
        Toast toast = Toast.makeText(this, "Copied to Clipboard", Toast.LENGTH_SHORT);
        toast.show();
    }

    class TranslateTask extends AsyncTask<Void, Void, List<List<String>>> {
        String content;

        @Override
        protected void onPreExecute() {
            TextInputEditText text_input = findViewById(R.id.original_text);
            content = String.valueOf(text_input.getText());

        }

        @Override
        protected List<List<String>> doInBackground(Void... params) {
            StringBuilder result = new StringBuilder();
            Map<String, Integer> words = new HashMap<>(); // store json data (lemmas) in a map
            List<List<String>> return_data = new ArrayList<>(); // return data for further use
            List<String> translation = new ArrayList<>(); // translated text
            List<String> words_en = new ArrayList<>(); // english word list
            List<String> words_fr = new ArrayList<>(); // french word list
            List<String> words_random_fr = new ArrayList<>(); // word list containing random french words

            if (content == null || content.equals("")) {
                translation.add(result.toString());
                return_data.add(translation);
                return_data.add(words_en);
                return_data.add(words_fr);
                return_data.add(words_random_fr);
                return return_data;
            }
            // get french translation result from Google Translate API(.cn is free)
            try {
                String googleResult = "";
                URL url = new URL("https://translate.google.cn/" + "translate_a/single?client=gtx&sl=en&tl=fr&dt=t&q=" + URLEncoder.encode(content, "UTF-8"));
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                urlConn.setConnectTimeout(5 * 1000);
                urlConn.setReadTimeout(5 * 1000);
                urlConn.setUseCaches(false);
                urlConn.setRequestMethod("GET");
                urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
                urlConn.connect();
                int statusCode = urlConn.getResponseCode();
                if (statusCode == 200) {
                    googleResult = streamToString(urlConn.getInputStream());
                }
                urlConn.disconnect();

                JSONArray jsonArray = new JSONArray(googleResult).getJSONArray(0);
                for (int i = 0; i < jsonArray.length(); i++) {
                    result.append(jsonArray.getJSONArray(i).getString(0));
                }
                // lemmatize the translation result with Twinword Lemmatizer API($0.003 per query)
                String lemmatizerApiKey = BuildConfig.RAPIDAPI_KEY;
                if (lemmatizerApiKey != null && !lemmatizerApiKey.trim().isEmpty()) {
                    String lemmatizerResult = "";
                    url = new URL("https://twinword-lemmatizer1.p.rapidapi.com/extract/?" +
                            "rapidapi-key=" + URLEncoder.encode(lemmatizerApiKey, "UTF-8") +
                            "&text=" + URLEncoder.encode(content, "UTF-8"));
                    urlConn = (HttpURLConnection) url.openConnection();
                    urlConn.setConnectTimeout(5 * 1000);
                    urlConn.setReadTimeout(5 * 1000);
                    urlConn.setUseCaches(false);
                    urlConn.setRequestMethod("GET");
                    urlConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
                    urlConn.connect();
                    statusCode = urlConn.getResponseCode();
                    if (statusCode == 200) {
                        lemmatizerResult = streamToString(urlConn.getInputStream());
                    }
                    urlConn.disconnect();

                    if (!lemmatizerResult.equals("")) {
                        JSONObject jsonObject = new JSONObject(lemmatizerResult).getJSONObject("lemma");
                        Gson gson = new Gson();
                        words = gson.fromJson(jsonObject.toString(), new TypeToken<Map<String,Integer>> () {}.getType());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                result = new StringBuilder();
            }

            /*Log.d("TAG", "result：" + result);
            for (String key: words.keySet()){
                Log.d("TAG", "key：" + key + " count: " + words.get(key));
            }*/

            translation.add(result.toString());
            return_data.add(translation);
            words_en = new ArrayList<>(words.keySet());
            //Log.d("TAG", "" + words_en);


            // filter words above a certain rank with SQLite database
            SQLiteDatabase db = wordDatabaseHelper.getWritableDatabase();
            List<String> query = new ArrayList<>();
            query.add(Integer.toString(vocabulary));
            StringBuilder word_query = new StringBuilder("(");
            int i = 0;
            for (String word: words_en){
                query.add(word);
                if (i == words_en.size() - 1){
                    word_query.append("?");
                }
                else{
                    word_query.append("?,");
                }
                i++;
            }
            word_query.append(")");
            Log.d("TAG", "result：" + word_query);
            String[] arr = new String[query.size()];
            Cursor cursor = db.query("WORDLIST",
                    new String[] {"_id", "ENGLISH", "FRENCH"},
                    "_id > ? AND ENGLISH IN " + word_query,
                    query.toArray(arr),null,null,null);

            List<String> words_en_filtered = new ArrayList<>();
            while (cursor.moveToNext()) {
                String english = cursor.getString(1);
                String french = cursor.getString(2);
                words_en_filtered.add(english);
                words_fr.add(french);
            }
            // if sick_mode is on, only take the first 3 words
            if (sick_mode && words_en_filtered.size() > limit)
            {
                words_en_filtered = words_en_filtered.subList(0, 3);
            }
            // We need to get random french words to generate the second MCQ challenge
            if (difficulty == 2){
                List<String> new_query = new ArrayList<>();
                Random rand = new Random();
                StringBuilder new_word_query = new StringBuilder("(");
                int k = words_en.size() * 2;
                for (int j = 0; j < k; j++){
                    String id = String.valueOf(rand.nextInt(17500 - vocabulary) + vocabulary);
                    new_query.add(id);
                    if (j == k - 1){
                        new_word_query.append("?");
                    }
                    else{
                        new_word_query.append("?,");
                    }
                }
                new_word_query.append(")");
                Log.d("TAG", "result：" + new_word_query);
                String[] arr_2 = new String[new_query.size()];
                cursor = db.query("WORDLIST",
                    new String[] {"_id", "FRENCH"},
                    "_id IN " + new_word_query,
                    new_query.toArray(arr_2),null,null,null);
                while (cursor.moveToNext()) {
                    String french = cursor.getString(1);
                    words_random_fr.add(french);
                }
            }
            cursor.close();
            return_data.add(words_en_filtered);
            return_data.add(words_fr);
            return_data.add(words_random_fr);
            /*String[] test = query.toArray(arr);
            i = 0;
            while (i < test.length)
            {
                Log.d("TAG", "result：" + test[i]);
                i++;
            }
            Log.d("TAG", "result：" + words_en_filtered + words_fr+words_en+words_random_fr);
            */
            return return_data;
        }

        @Override
        protected void onPostExecute(List<List<String>> result) {

           if (difficulty == 1) {
               showFirstChallenge(result);
           }
           else if (difficulty == 2){
               showSecondChallenge(result);
            }
           else{
               showThirdChallenge(result);
           }
           // show the translation result
            TextView text_output = findViewById(R.id.target_text);
            text_output.setText(result.get(0).get(0));
            // make the textview scrollable
            text_output.setMovementMethod(ScrollingMovementMethod.getInstance());


        }

    }
    private void showFirstChallenge(List<List<String>> result){
        View popView = LayoutInflater.from(this).inflate(R.layout.first_popup, null);
        ListView word_list = popView.findViewById(R.id.wordListView);
        TextView message = popView.findViewById(R.id.message);
        List<Map<String,String>> items = new ArrayList<>();
        List<String> en_list = result.get(1);
        List<String> fr_list = result.get(2);
        for (int i = 0; i < en_list.size(); i++)
        {
            Map<String, String> rendered = new HashMap<>();
            rendered.put("EN", en_list.get(i));
            rendered.put("FR", fr_list.get(i));
            items.add(rendered);
        }
        SimpleAdapter myAdapter = new SimpleAdapter(getApplicationContext(), items, R.layout.first_challenge,
                new String[] {"EN", "FR"}, new int[] {R.id.list_en, R.id.list_fr});
        word_list.setAdapter(myAdapter);
        int word_number = en_list.size();
        if (word_number == 1){
            message.setText(getResources().getString(R.string.prompt));
        }
        else if (word_number > 1){
            message.setText("Here are the " + word_number + " words you may want to have a look at");
        }
        else{
            message.setText(getResources().getString(R.string.error_message));
        }

        PopupWindow popupWindow = new PopupWindow(popView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.showAtLocation(findViewById(android.R.id.content).getRootView(), Gravity.NO_GRAVITY, 0, 0);


    }

    private void showSecondChallenge(List<List<String>> result) {

        View popView = LayoutInflater.from(this).inflate(R.layout.third_popup, null);
        ViewPager word_challenges = popView.findViewById(R.id.vp);
        List<String> en_list = result.get(1);
        List<String> fr_list = result.get(2);
        List<String> fr_random = result.get(3);
        List<View> challenges = new ArrayList<>();
        LayoutInflater inflater = getLayoutInflater();
        int j = en_list.size();
        for (int i = 0; i < j; i++) {
            View challenge = inflater.inflate(R.layout.third_challenge, null, false);
            String answer = fr_list.get(i);
            TextView word = challenge.findViewById(R.id.word);
            word.setText(en_list.get(i));
            Button option_1 = challenge.findViewById(R.id.option_1);
            Button option_2 = challenge.findViewById(R.id.option_2);
            Button option_3 = challenge.findViewById(R.id.option_3);
            List<Button> buttons = new ArrayList<>();
            buttons.add(option_1);
            buttons.add(option_2);
            buttons.add(option_3);
            List<String> options = new ArrayList<>();
            options.add(answer);
            options.add(fr_random.get(2 * i));
            options.add(fr_random.get(2 * i + 1));
            Collections.shuffle(options);
            int green = getResources().getColor(R.color.green);
            int red = getResources().getColor(R.color.red);
            int k;
            for (k = 0; k < 3; k++){
                Button button = buttons.get(k);
                String option = options.get(k);
                button.setText(option);
                button.setOnClickListener(v -> {
                    if (option.equals(answer)){
                        button.setBackgroundColor(green);
                    }
                    else{
                        button.setBackgroundColor(red);
                    }
                });
            }
            challenges.add(challenge);
        }
        if (en_list.size() == 0){
            View message = inflater.inflate(R.layout.message, null, false);
            TextView prompt = message.findViewById(R.id.message);
            prompt.setText(getResources().getString(R.string.error_message));
            challenges.add(message);
        }
        MyPagerAdapter adapter = new MyPagerAdapter(challenges);
        word_challenges.setAdapter(adapter);

        PopupWindow popupWindow = new PopupWindow(popView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(findViewById(android.R.id.content).getRootView(), Gravity.NO_GRAVITY, 0, 0);



    }

    public class MyPagerAdapter extends PagerAdapter {
        private List<View> viewLists;

        public MyPagerAdapter(List<View> viewLists) {
            super();
            this.viewLists = viewLists;
        }

        @Override
        public int getCount() {
            return viewLists.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(viewLists.get(position));
            return viewLists.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(viewLists.get(position));
        }
    }

    private void showThirdChallenge(List<List<String>> result){
        View popView = LayoutInflater.from(this).inflate(R.layout.second_popup, null);
        ListView word_list = popView.findViewById(R.id.challengeView);
        TextView message = popView.findViewById(R.id.message);
        List<String> en_list = result.get(1);
        List<String> fr_list = result.get(2);
        ThirdChallengeAdapter adapter = new ThirdChallengeAdapter(en_list, fr_list, MainActivity.this);
        word_list.setAdapter(adapter);
        int word_number = en_list.size();
        if (word_number == 1){
            message.setText(getResources().getString(R.string.prompt));
        }
        else if (word_number > 1){
            message.setText("Here are the " + word_number + " words you may want to have a guess at");
        }
        else{
            message.setText(getResources().getString(R.string.error_message));
        }

        PopupWindow popupWindow = new PopupWindow(popView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(findViewById(android.R.id.content).getRootView(), Gravity.NO_GRAVITY, 0, 0);

    }

    public class ThirdChallengeAdapter extends BaseAdapter {

        private List<String> en_list;
        private List<String> fr_list;
        private Context mContext;

        public ThirdChallengeAdapter(List<String> en_list, List<String> fr_list, Context mContext) {
            this.en_list = en_list;
            this.fr_list = fr_list;
            this.mContext = mContext;
        }

        @Override
        public int getCount() {
            return en_list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.second_challenge, parent, false);
            TextView en_word = convertView.findViewById(R.id.word_en);
            TextInputEditText input = convertView.findViewById(R.id.word_text);
            Button button = convertView.findViewById(R.id.button2);
            String english = en_list.get(position);
            String french = fr_list.get(position);
            en_word.setText(english);
            button.setOnClickListener(v -> {
                String user_input = String.valueOf(input.getText());
                int green = getResources().getColor(R.color.green);
                int red = getResources().getColor(R.color.red);
                if (user_input.equals(french)){
                    button.setBackgroundColor(green);
                    Toast.makeText(MainActivity.this, "Correct!", Toast.LENGTH_SHORT).show();

                }
                else{
                    button.setBackgroundColor(red);
                    Toast.makeText(MainActivity.this, "The answer is not correct, try again!", Toast.LENGTH_SHORT).show();
                }
            });

            return convertView;
        }
    }

    public static String streamToString(InputStream is) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.close();
            is.close();
            return out.toString();
        } catch (Exception e) {
            return null;
        }
    }
// hide the keyboard when the focus is lost
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {

            View v = getCurrentFocus();
            if (isShouldHideKeyboard(v, ev)) {

                hideKeyboard(v.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isShouldHideKeyboard(View v, MotionEvent event) {

        if (v instanceof EditText) {

            int[] l = {
                    0, 0};
            v.getLocationInWindow(l);
            int left = l[0],
                    top = l[1],
                    bottom = top + v.getHeight(),
                    right = left + v.getWidth();
            return !(event.getX() > left) || !(event.getX() < right)
                    || !(event.getY() > top) || !(event.getY() < bottom);
        }
        return false;
    }

    private void hideKeyboard(IBinder token) {

        textInputEditText.clearFocus();
        if (token != null) {

            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (im != null) {

                im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
}
