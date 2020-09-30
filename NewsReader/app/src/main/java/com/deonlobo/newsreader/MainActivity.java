package com.deonlobo.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> title = new ArrayList<String>();
    ArrayList<String> contents = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content", contents.get(position));

                startActivity(intent);

            }
        });

        arrayAdapter = new ArrayAdapter(this , android.R.layout.simple_list_item_1 , title);

        listView.setAdapter(arrayAdapter);

        articlesDB =this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY , articlesId INTEGER ,title VARCHAR,content VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();
        try {

            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void updateListView(){

        Cursor c= articlesDB.rawQuery("Select * from articles",null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()){

            title.clear();
            contents.clear();

            do{

                title.add(c.getString(titleIndex));
                contents.add(c.getString(contentIndex));

            }while(c.moveToNext());

            arrayAdapter.notifyDataSetChanged();

        }


    }

    public class DownloadTask extends AsyncTask<String , Void , String>{

        @Override
        protected String doInBackground(String... params) {

            URL url ;
            HttpURLConnection urlConnection = null;

            try {
                String result= ""; // Init inside the try, so its safe
                
                url = new URL(params[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1){

                    char curr = (char) data;
                    result += curr;
                    data = reader.read();


                }
                JSONArray jsonArray = new JSONArray(result);
                int noOfItems = 20;

                if(jsonArray.length()<20){
                    noOfItems = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM articles");

                for(int i=0;i<noOfItems;i++){
                    System.out.println(i);
                    String articleId =jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();
                    String articleInfo = "";

                    while(data!= -1){

                        char curr = (char) data;
                        articleInfo += curr;
                        data= reader.read();

                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")) {

                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");
                        System.out.println(articleTitle+articleUrl);

                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();

                        BufferedReader r = new BufferedReader(new InputStreamReader(in));
                        StringBuilder str = new StringBuilder();
                        String line = null;
                        while((line = r.readLine()) != null)
                        {
                            str.append(line);
                        }
                        in.close();
                        String articleContent = str.toString();


                        String sql = "INSERT INTO articles (articlesId,title,content) VALUES (?,?,?)";

                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);

                        statement.execute();

                    }


                }

                return result;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
            articlesDB.close();
        }
    }

}
