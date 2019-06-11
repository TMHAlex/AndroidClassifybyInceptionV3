package com.example.makeapicture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.*;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.FileInputStream;
import java.io.InputStream;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    /*
     * 在需要调用TensoFlow的地方，加载so库“System.loadLibrary("tensorflow_inference");
     * 并”import org.tensorflow.contrib.android.TensorFlowInferenceInterface;就可以使用了
     * */
    //Load the tensorflow inference library
    //static{}(即static块)，会在类被加载的时候执行且仅会被执行一次，一般用来初始化静态变量和调用静态方法。
    static {
        System.loadLibrary("tensorflow_inference");
    }

    //PATH TO OUR MODEL FILE AND NAMES OF THE INPUT AND OUTPUT NODES
    //各节点名称
    private String MODEL_PATH = "inception_v4_299_quant_frozen.pb";
    private String INPUT_NAME = "input";
    private String OUTPUT_NAME = "InceptionV4/Logits/Predictions";
    private TensorFlowInferenceInterface tf;

    //ARRAY TO HOLD THE PREDICTIONS AND FLOAT VALUES TO HOLD THE IMAGE DATA
    //保存图片和图片尺寸的
    float[] PREDICTIONS = new float[1002];
    private float[] floatValues;
    private int[] INPUT_SIZE = {299, 299, 3};

    ImageView imageView;
    TextView resultView;
    Button buttonSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tf = new TensorFlowInferenceInterface(getAssets(), MODEL_PATH);

        imageView = (ImageView) findViewById(R.id.imageView1);
        resultView = (TextView) findViewById(R.id.text_show);
        buttonSub = (Button) findViewById(R.id.button1);

        buttonSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    InputStream imageStream = getAssets().open("airplane.jpg");
                    Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                    imageView.setImageBitmap(bitmap);

                    predict(bitmap);

                }catch(Exception e){

                }
            }
        });
    }

    public ArrayList<Object[]> argmax(float[] array){

        /*int best = -1;
        float best_confidence = 0.0f;
        for(int i = 0;i < array.length;i++){
            float value = array[i];
            if (value > best_confidence){
                best_confidence = value;
                best = i;
            }
        }
        return new Object[]{best,best_confidence};*/
        ArrayList<Object[]> topFivePredict = new ArrayList<Object[]>();
        for(int findfive=0;findfive<5;findfive++)
        {
            int best = -1;
            float best_confidence = 0.0f;
            for(int i = 0;i < array.length;i++){
                float value = array[i];
                if (value > best_confidence){
                    best_confidence = value;
                    best = i;
                }
            }
            array[best]=0;
            Log.d("bestvalue",String.valueOf(best_confidence));
            Object[] Result = new Object[]{best,best_confidence};
            topFivePredict.add(Result);
        }
        return topFivePredict;
    }



    public void predict(final Bitmap bitmap){

        //Runs inference in background thread
        new AsyncTask<Integer,Integer,Integer>(){

            @Override
            protected Integer doInBackground(Integer ...params){
                String showTopFive = "";
                //Resize the image into 224 x 224
                //mobilenet 224 inceptionV1V2 244 inceptionV3V4 299
                Bitmap resized_image = ImageUtils.processBitmap(bitmap,299);

                //Normalize the pixels
                floatValues = ImageUtils.normalizeBitmap(resized_image,299,127.5f,1.0f);

                //Pass input into the tensorflow
                tf.feed(INPUT_NAME,floatValues,1,299,299,3);

                //compute predictions
                tf.run(new String[]{OUTPUT_NAME});

                //copy the output into the PREDICTIONS array
                tf.fetch(OUTPUT_NAME,PREDICTIONS);

                //Obtained highest prediction
                ArrayList<Object[]> top5Results = argmax(PREDICTIONS);

                for(int i=0;i<5;i++)
                {
                    Object[] results = top5Results.get(i);
                    int class_index = (Integer) results[0];
                    float confidence = (Float) results[1];
                    try{
                        final String conf = String.valueOf(confidence * 100).substring(0,5);
                        //final String conf = String.valueOf(confidence).substring(0,5);
                        //Convert predicted class index into actual label name
                        final String label = ImageUtils.getLabel(getAssets().open("labels.json"),class_index);
                        showTopFive = showTopFive + label + " : " + conf + "%"+"\n";

                    } catch (Exception e){
                    }
                }
                try{
                    final String finalShowTopFive = showTopFive;
                    //Display result on UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText(finalShowTopFive);
                        }
                    });
                }catch (Exception e) {
                }
                return 0;
            }
        }.execute(0);

    }
}