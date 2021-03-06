package com.qingmei2.rximagepicker.core;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import com.qingmei2.rximagepicker.di.scheduler.IRxImagePickerSchedulers;
import com.qingmei2.rximagepicker.funtions.ObserverAsConverter;
import com.qingmei2.rximagepicker.ui.ICameraCustomPickerView;
import com.qingmei2.rximagepicker.ui.IGalleryCustomPickerView;

import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * {@link ImagePickerConfigProcessor} is the class that processing reactive data stream.
 */
public final class ImagePickerConfigProcessor implements
        IImagePickerProcessor {

    @VisibleForTesting
    public final Context context;
    @VisibleForTesting
    public final Map<String, ICameraCustomPickerView> cameraViews;
    @VisibleForTesting
    public final Map<String, IGalleryCustomPickerView> galleryViews;
    @VisibleForTesting
    public final IRxImagePickerSchedulers schedulers;

    public ImagePickerConfigProcessor(Context context,
                                      Map<String, ICameraCustomPickerView> cameraViews,
                                      Map<String, IGalleryCustomPickerView> galleryViews,
                                      IRxImagePickerSchedulers schedulers) {
        this.context = context;
        this.cameraViews = cameraViews;
        this.galleryViews = galleryViews;
        this.schedulers = schedulers;
    }

    @Override
    public Observable<?> process(ImagePickerConfigProvider configProvider) {
        return Observable.just(configProvider)
                .flatMap(sourceFrom(cameraViews, galleryViews))
                .flatMap(observerAs(configProvider, context))
                .subscribeOn(schedulers.io())
                .observeOn(schedulers.ui());
    }

    @VisibleForTesting
    public Function<ImagePickerConfigProvider, ObservableSource<Uri>> sourceFrom(
            Map<String, ICameraCustomPickerView> cameraViews,
            Map<String, IGalleryCustomPickerView> galleryViews) {
        return new Function<ImagePickerConfigProvider, ObservableSource<Uri>>() {
            @Override
            public ObservableSource<Uri> apply(ImagePickerConfigProvider provider) throws Exception {
                if (provider.isSingleActivity()) {
                    return ActivityPickerProjector.getInstance().pickImage();
                }
                switch (provider.getSourcesFrom()) {
                    case GALLERY:
                    case CAMERA:
                        return provider.getPickerView().pickImage();
                    default:
                        throw new IllegalArgumentException("unknown SourceFrom data.");
                }
            }
        };
    }

    @VisibleForTesting
    public Function<Uri, ObservableSource<?>> observerAs(
            ImagePickerConfigProvider configProvider,
            Context context) {
        return new ObserverAsConverter(configProvider.getObserverAs(), context);
    }
}
