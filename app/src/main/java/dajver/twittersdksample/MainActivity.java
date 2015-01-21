package dajver.twittersdksample;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class MainActivity extends Activity {

    //наши ключ и секрет
    static String TWITTER_CONSUMER_KEY = "uzm61o57KS3MapC7YnrmBLqzV";
    static String TWITTER_CONSUMER_SECRET = "cvx8GRw2irqxd5PMfLyDO3mriT4k0S5HJPEcpJoCzgCwVEaMzu";

    //ну тут разные переменные для работы с апи
    private Twitter twitter;
    private RequestToken requestToken = null;
    private AccessToken accessToken;
    private String oauth_url,oauth_verifier;
    private Dialog auth_dialog;
    private WebView web;
    private ProgressDialog progress;

    private TextView name;
    private String nameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //инициализируем их и присваиваем ключи  что бы апа знало с чем ему работать
        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(TWITTER_CONSUMER_KEY, TWITTER_CONSUMER_SECRET);

        //объявляем нашу кнопку и вызываем наш асинк таск которые создает запрос к апи
        Button authBtn = (Button) findViewById(R.id.button);
        name = (TextView) findViewById(R.id.textView);

        authBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //вызываем асинк таск для выозова диалога с вебвью
                new TokenGet().execute();
            }
        });
    }

    //получаем токен через этот запрос и открываем диалог с вебвью
    private class TokenGet extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... args) {

            try {
                requestToken = twitter.getOAuthRequestToken();
                oauth_url = requestToken.getAuthorizationURL();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return oauth_url;
        }

        @Override
        protected void onPostExecute(String oauth_url) {
            // запускаем диалог с вебвью
            if(oauth_url != null){
                auth_dialog = new Dialog(MainActivity.this);
                auth_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                auth_dialog.setContentView(R.layout.dialog_twitter_auth);
                web = (WebView)auth_dialog.findViewById(R.id.webv);
                web.getSettings().setJavaScriptEnabled(true);
                web.loadUrl(oauth_url);
                web.setWebViewClient(new WebViewClient() {

                    //если уже до этого логинились то показываем экран не с полями для ввода,
                    // а просто кнопку авторизироваться
                    boolean authComplete = false;

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon){
                        super.onPageStarted(view, url, favicon);
                    }

                    //закрываем диалог после получения ответа "ок"
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        //если все ок то авторизируем и закрываем диалог если все плохо
                        // и запрос не верный, или пароль или нет инета или что то еще
                        //тогда закрываем диалог и тихо истерим
                        if (url.contains("oauth_verifier") && authComplete == false){
                            authComplete = true;
                            Uri uri = Uri.parse(url);
                            oauth_verifier = uri.getQueryParameter("oauth_verifier");
                            auth_dialog.dismiss();
                            //делаем запрос к
                            new AccessTokenGet().execute();
                        } else if(url.contains("denied")){
                            auth_dialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Sorry !, Permission Denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                auth_dialog.show();
                auth_dialog.setCancelable(true);
            }else{
                Toast.makeText(getApplicationContext(), "Sorry !, Network Error or Invalid Credentials", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //а в этом потоке мы создаем запрос к апи на получение данных
    private class AccessTokenGet extends AsyncTask<String, String, Boolean> {

        // создаем и показываем загрузочный прогрес диалог
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(MainActivity.this);
            progress.setMessage("Fetching Data ...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();
        }

        // по полученым токену в потоке выше иы получаем возможность
        // получить данные поюзеру, его ися его аватарку и т д.
        @Override
        protected Boolean doInBackground(String... args) {
            try {
                accessToken = twitter.getOAuthAccessToken(requestToken, oauth_verifier);
                //получаем данные по юзеру
                User user = twitter.showUser(accessToken.getUserId());
                // говнокод так не делайте потому что я это делал для примера и решил что мне можно)
                // но вообще лучше сохранить в преференсы или передать на другой экран или еще что то
                // для примера прокатит. А сделал я так потому что работа с UI в этом потоке запрещена
                //вся работа с ui должна быть в главном потоке
                nameText = user.getName();
                runOnUiThread(new Runnable(){
                    @Override
                    public void run(){
                        name.setText(nameText);
                    }
                });
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return true;
        }

        //прячем загрузочный диалог
        //здесь например можно сделать переход на следующий экран после окончания загрузки
        // данных из твиттера, или еще что то что вам хочется.
        @Override
        protected void onPostExecute(Boolean response) {
            if(response){
                progress.hide();
            }
        }
    }
}
