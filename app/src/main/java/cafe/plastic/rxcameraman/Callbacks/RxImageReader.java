package cafe.plastic.rxcameraman.Callbacks;


import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import java.nio.ByteBuffer;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;


public class RxImageReader {
    public static final String TAG = RxImageReader.class.getSimpleName();

    public static Flowable<byte[]> getImage(ImageReader reader) {
        Flowable<byte[]> imageFlowable = Flowable.create(new FlowableOnSubscribe<byte[]>() {
            @Override
            public void subscribe(FlowableEmitter<byte[]> emitter) throws Exception {
                reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        Log.d(TAG, "Picture taken.");
                        if (imageReader.getMaxImages() == 0) return;
                        Image image = imageReader.acquireLatestImage();
                        final Image.Plane[] planes = image.getPlanes();
                        final ByteBuffer buffer = planes[0].getBuffer();
                        final byte[] data = new byte[buffer.capacity()];
                        buffer.get(data);
                        image.close();
                        emitter.onNext(data);
                    }
                }, null);
            }

        }, BackpressureStrategy.DROP).share();
        return imageFlowable;
    }
}
