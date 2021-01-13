package eu.kanade.tachiyomi.ui.reader.viewer.pager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.SparseArray;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.mlsdk.MLAnalyzerFactory;
import com.huawei.hms.mlsdk.common.MLApplication;
import com.huawei.hms.mlsdk.common.MLFrame;
import com.huawei.hms.mlsdk.text.MLLocalTextSetting;
import com.huawei.hms.mlsdk.text.MLRemoteTextSetting;
import com.huawei.hms.mlsdk.text.MLText;
import com.huawei.hms.mlsdk.text.MLTextAnalyzer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class OCRHintsImageView extends SubsamplingScaleImageView {
    private SparseArray<MLText.Block> OCR;
    private ResultIterator OCRResult;
    private List<RectF> textLocations = new ArrayList<>();
    private List<PointF> topLefts = new ArrayList<>();
    private List<PointF> botRights = new ArrayList<>();
    private Paint paint = new Paint();
    private final PagerViewer viewerRef;
    private boolean showHints = false;
    private DoOCRTask OCRTask;
    private MLText RemoteOCR;
    private static boolean coso = false;

    public OCRHintsImageView(@Nullable Context context, @NotNull PagerViewer viewer, InputStream stream) {
        super(context);
        viewerRef = viewer;
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.rgb(89, 126, 255));
        float density = getResources().getDisplayMetrics().densityDpi;
        paint.setStrokeWidth((int)(density/60f));
        OCRTask = new DoOCRTask(this);
        OCRTask.execute(stream);
        /*huaweiRemoteOCR(stream);*/
        /*OCRTask = new DoHuaweiOCRTask(this);
        OCRTask.execute(stream);*/
    }

    private void huaweiRemoteOCR(InputStream stream) {
        if(!coso) {
            coso = true;
            MLApplication.getInstance().setApiKey("CgB6e3x9QwmjgWLAX1djsfumeZa/HJFS8rTlsfFBYF54dcaTZpbGUlVVIQg/zKvcL6OVK6a4acXzjd9rSMCubcvR");
        }
        List<String> languageList = new ArrayList();
        languageList.add("ja");
        // Set parameters.
        MLRemoteTextSetting setting = new MLRemoteTextSetting.Factory()
                // Set the on-cloud text detection mode.
                // MLRemoteTextSetting.OCR_COMPACT_SCENE: dense text recognition
                // MLRemoteTextSetting.OCR_LOOSE_SCENE: sparse text recognition
                .setTextDensityScene(MLRemoteTextSetting.OCR_LOOSE_SCENE)
                // Specify the languages that can be recognized, which should comply with ISO 639-1.
                .setLanguageList(languageList)
                // Set the format of the returned text border box.
                // MLRemoteTextSetting.NGON: Return the coordinates of the four corner points of the quadrilateral.
                // MLRemoteTextSetting.ARC: Return the corner points of a polygon border in an arc. The coordinates of up to 72 corner points can be returned.
                .setBorderType(MLRemoteTextSetting.NGON)
                .create();
        MLTextAnalyzer analyzer = MLAnalyzerFactory.getInstance().getRemoteTextAnalyzer(setting);
        MLFrame frame = MLFrame.fromBitmap(BitmapFactory.decodeStream(stream));
        Task<MLText> task = analyzer.asyncAnalyseFrame(frame);
        task.addOnSuccessListener(this::setOCR);
    }

    private void setOCR(MLText mlText) {
        RemoteOCR = mlText;
        for (MLText.Block block : mlText.getBlocks()) {
            textLocations.add(new RectF(block.getBorder()));
            topLefts.add(new PointF(block.getVertexes()[0]));
            botRights.add(new PointF(block.getVertexes()[2]));
        }
        animateHints();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(!isReady() || !showHints /*|| RemoteOCR == null*/|| OCRResult == null/*|| OCR == null*/) return;

        /*for(int i = 0; i < textLocations.size(); i++) {
            PointF tl = topLefts.get(i);
            PointF br = botRights.get(i);
            MLText.Block block = RemoteOCR.getBlocks().get(i);
            tl.set(block.getVertexes()[0].x, block.getVertexes()[0].y);
            br.set(block.getVertexes()[2].x, block.getVertexes()[2].y);
            sourceToViewCoord(tl, tl);
            sourceToViewCoord(br, br);
            RectF location = textLocations.get(i);
            location.set(tl.x, tl.y, br.x, br.y);
            canvas.drawRect(location, paint);
        }*/
        int level = TessBaseAPI.PageIteratorLevel.RIL_BLOCK;
        int i = 0;
        OCRResult.begin();
        do {
            Rect boundingRect = OCRResult.getBoundingRect(level);
            textLocations.add(new RectF(boundingRect));
            PointF tl = topLefts.get(i);
            PointF br = botRights.get(i);
            Rect rect = OCRResult.getBoundingRect(level);
            tl.set(rect.left, rect.top);
            br.set(rect.right, rect.bottom);
            sourceToViewCoord(tl, tl);
            sourceToViewCoord(br, br);
            RectF location = textLocations.get(i);
            location.set(tl.x, tl.y, br.x, br.y);
            canvas.drawRect(location, paint);
            i++;
        } while(OCRResult.next(level));
    }

    private void setOCR(SparseArray<MLText.Block> OCR) {
        this.OCR = OCR;
        for (int i = 0; i < OCR.size(); i++) {
            MLText.Block block = OCR.valueAt(i);
            textLocations.add(new RectF(block.getBorder()));
            topLefts.add(new PointF(block.getVertexes()[0]));
            botRights.add(new PointF(block.getVertexes()[2]));
        }
        animateHints();
    }

    private void setOCR(ResultIterator resultIterator) {
        OCRResult = resultIterator;
        int level = TessBaseAPI.PageIteratorLevel.RIL_BLOCK;
        resultIterator.begin();
        do {
            Rect boundingRect = resultIterator.getBoundingRect(level);
            textLocations.add(new RectF(boundingRect));
            topLefts.add(new PointF());
            botRights.add(new PointF());
        } while(resultIterator.next(level));
        animateHints();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            for (RectF location : textLocations) {
                if(location.contains(event.getX() - getLeft(), event.getY() - getTop())) {
                    viewerRef.getPager().setGestureDetectorEnabled(false);
                    return true;
                }
            }
            // Tocó pero en otro lado, mostremos los cuadraditos un rato por las dudas
            if(!viewerRef.getActivity().getMenuVisible())
                animateHints();
        }
        viewerRef.getPager().setGestureDetectorEnabled(true);
        return false;
    }

    private void animateHints() {
        showHints = true;
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator fadeIn = ObjectAnimator.ofInt(paint, "alpha", 0, 200).setDuration(300);
        fadeIn.addUpdateListener(animation -> invalidate());
        ObjectAnimator fadeOut = ObjectAnimator.ofInt(paint, "alpha", 200, 0).setDuration(1200);
        fadeOut.addUpdateListener(animation -> invalidate());
        animatorSet.play(fadeIn).before(fadeOut);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showHints = false;
            }
        });
        animatorSet.start();
    }

    private class DoOCRTask extends AsyncTask<InputStream, Void, ResultIterator> {
        OCRHintsImageView viewRef;
        Context contextRef;

        DoOCRTask(OCRHintsImageView ref) {
            viewRef = ref;
            contextRef = ref.getContext();
        }

        @Override
        protected ResultIterator doInBackground(InputStream... inputStreams) {
            /*MLLocalTextSetting setting = new MLLocalTextSetting.Factory()
                    .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE) // Specify languages that can be recognized.
                    .setLanguage("ja")
                    .create();
            MLTextAnalyzer analyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(setting);
            MLFrame frame = MLFrame.fromBitmap(BitmapFactory.decodeStream(inputStreams[0]));
            return analyzer.analyseFrame(frame);*/
            File dir = Environment.getExternalStorageDirectory();
            File tessdata = new File(dir.getPath() + "/tessdata");
            if (!tessdata.exists()) {
                tessdata.mkdirs();
                copyAssetFolderToFolder(contextRef, "tessdata", tessdata);
            }
            TessBaseAPI api = new TessBaseAPI();
            api.init(dir.getPath(),"Japanese_vert", TessBaseAPI.OEM_LSTM_ONLY);
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT);
            api.setImage(BitmapFactory.decodeStream(inputStreams[0]));
            String texto = api.getUTF8Text();
            ResultIterator result = api.getResultIterator();
            api.end();
            return result;
        }

        public void copyAssetFolderToFolder(Context activity, String assetsFolder, File destinationFolder) {
            InputStream stream = null;
            OutputStream output = null;
            try {
                for (String fileName : activity.getAssets().list(assetsFolder)) {
                    stream = activity.getAssets().open(assetsFolder + "/" + fileName);
                    output = new BufferedOutputStream(new FileOutputStream(new File(destinationFolder, fileName)));

                    byte data[] = new byte[1024];
                    int count;

                    while ((count = stream.read(data)) != -1) {
                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    stream.close();

                    stream = null;
                    output = null;
                }
            } catch (/*any*/Exception e){e.printStackTrace();}
        }

        @Override
        protected void onPostExecute(ResultIterator resultIterator) {
            viewRef.setOCR(resultIterator);
        }
    }

    private class DoHuaweiOCRTask extends AsyncTask<InputStream, Void, SparseArray<MLText.Block>> {
        OCRHintsImageView viewRef;
        Context contextRef;

        DoHuaweiOCRTask(OCRHintsImageView ref) {
            viewRef = ref;
            contextRef = ref.getContext();
        }

        @Override
        protected SparseArray<MLText.Block> doInBackground(InputStream... inputStreams) {
            MLTextAnalyzer analyzer = new MLTextAnalyzer.Factory(contextRef).setLanguage("ja").create();
            return analyzer.analyseFrame(MLFrame.fromBitmap(BitmapFactory.decodeStream(inputStreams[0])));
        }

        @Override
        protected void onPostExecute(SparseArray<MLText.Block> blockSparseArray) {
            viewRef.setOCR(blockSparseArray);
        }
    }

}
