package com.Tool.Function;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.Tool.Global.Constant;
import com.Tool.Global.Variable;
import com.czt.mp3recorder.util.LameUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import zty.composeaudio.Tool.Decode.DecodeEngine;
import zty.composeaudio.Tool.Interface.ComposeAudioInterface;
import zty.composeaudio.Tool.Interface.DecodeOperateInterface;
import zty.composeaudio.bean.RecordBean;

/**
 * Created by zhengtongyu on 16/5/29.
 */
public class AudioFunction {

    public static final int byteBufferSize = 1024;

    public static void DecodeMusicFile(final String musicFileUrl, final String decodeFileUrl, final int startSecond,
                                       final int endSecond,
                                       final DecodeOperateInterface decodeOperateInterface) {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                DecodeEngine.getInstance().beginDecodeMusicFile(musicFileUrl, decodeFileUrl, startSecond, endSecond,
                        decodeOperateInterface);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        LogFunction.error("异常观察", e.toString());
                    }

                    @Override
                    public void onNext(String string) {
                    }
                });
    }

    /**
     * @param firstAudioPath
     * @param secondAudioPath
     * @param composeFilePath       生成音频路径
     * @param composePcmUrl
     * @param deleteSource
     * @param firstAudioWeight      音频音量权重
     * @param secondAudioWeight     音频音量权重
     * @param audioOffset           混淆开始时间
     * @param composeAudioInterface
     */
    public static void BeginComposeAudio(final String firstAudioPath, final String secondAudioPath,
                                         final String composeFilePath, final String composePcmUrl, final boolean deleteSource,
                                         final float firstAudioWeight,
                                         final float secondAudioWeight, final int audioOffset,
                                         final ComposeAudioInterface composeAudioInterface) {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                ComposeAudio(firstAudioPath, secondAudioPath, composeFilePath, composePcmUrl, deleteSource,
                        firstAudioWeight, secondAudioWeight, audioOffset, composeAudioInterface);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(String string) {
                    }
                });
    }

    public static void ComposeAudio(String firstAudioFilePath, String secondAudioFilePath,
                                    String composeAudioFilePath, String composePcmUrl, boolean deleteSource,
                                    float firstAudioWeight, float secondAudioWeight,
                                    int audioOffset,
                                    final ComposeAudioInterface composeAudioInterface) {
        boolean firstAudioFinish = false;
        boolean secondAudioFinish = false;
        AACEncode aacEncode;

        byte[] firstAudioByteBuffer;
        byte[] secondAudioByteBuffer;
        byte[] mp3Buffer;

        short resultShort;
        short[] outputShortArray;

        int index;
        int firstAudioReadNumber;
        int secondAudioReadNumber;
        int outputShortArrayLength;
        final int byteBufferSize = 1024;
        String aacUrl = Variable.StorageDirectoryPath + "hello.aac";

        firstAudioByteBuffer = new byte[byteBufferSize];
        secondAudioByteBuffer = new byte[byteBufferSize];
        mp3Buffer = new byte[(int) (7200 + (byteBufferSize * 1.25))];

        outputShortArray = new short[byteBufferSize / 2];

        Handler handler = new Handler(Looper.getMainLooper());

        FileInputStream firstAudioInputStream = FileFunction.GetFileInputStreamFromFile
                (firstAudioFilePath);
        FileInputStream secondAudioInputStream = FileFunction.GetFileInputStreamFromFile
                (secondAudioFilePath);
        FileOutputStream composeAudioOutputStream = FileFunction.GetFileOutputStreamFromFile
                (composeAudioFilePath);
        FileOutputStream composePcmOutputStream = FileFunction.GetFileOutputStreamFromFile
                (composePcmUrl);
        FileOutputStream composeAACOutputStream = FileFunction.GetFileOutputStreamFromFile
                (aacUrl);

        LameUtil.init(Constant.RecordSampleRate, Constant.LameBehaviorChannelNumber,
                Constant.BehaviorSampleRate, Constant.LameBehaviorBitRate, Constant.LameMp3Quality);
        aacEncode = new AACEncode();
        try {
            Log.e("composeAudio", "start compose recode");
            while (!firstAudioFinish && !secondAudioFinish) {
                index = 0;
                Log.e("composeAudio", "start compose recode" + index);
                if (audioOffset < 0) {
                    secondAudioReadNumber = secondAudioInputStream.read(secondAudioByteBuffer);

                    outputShortArrayLength = secondAudioReadNumber / 2;

                    for (; index < outputShortArrayLength; index++) {
                        resultShort = CommonFunction.GetShort(secondAudioByteBuffer[index * 2],
                                secondAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                        outputShortArray[index] = (short) (resultShort * secondAudioWeight);
                    }

                    audioOffset += secondAudioReadNumber;

                    if (secondAudioReadNumber < 0) {
                        secondAudioFinish = true;
                        break;
                    }

                    if (audioOffset >= 0) {
                        break;
                    }
                } else {
                    firstAudioReadNumber = firstAudioInputStream.read(firstAudioByteBuffer);

                    outputShortArrayLength = firstAudioReadNumber / 2;

                    for (; index < outputShortArrayLength; index++) {
                        resultShort = CommonFunction.GetShort(firstAudioByteBuffer[index * 2],
                                firstAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                        outputShortArray[index] = (short) (resultShort * firstAudioWeight);
                    }

                    audioOffset -= firstAudioReadNumber;

                    if (firstAudioReadNumber < 0) {
                        firstAudioFinish = true;
                        break;
                    }

                    if (audioOffset <= 0) {
                        break;
                    }
                }

                if (outputShortArrayLength > 0) {

                    byte[] bytes = CommonFunction.GetByteBuffer(outputShortArray, Variable.isBigEnding);
                    composePcmOutputStream.write(bytes, 0, bytes.length);
                    byte[] offerEncoder = aacEncode.offerEncoder(bytes);
                    Log.e("offerEncoder", "offerEncoder Size:" + offerEncoder.length);
                    composeAACOutputStream.write(offerEncoder, 0, offerEncoder.length);

                    int encodedSize = LameUtil.encode(outputShortArray, outputShortArray,
                            outputShortArrayLength, mp3Buffer);

                    if (encodedSize > 0) {
                        composeAudioOutputStream.write(mp3Buffer, 0, encodedSize);
                    }
                }
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (composeAudioInterface != null) {
                        composeAudioInterface.updateComposeProgress(20);
                    }
                }
            });

            Log.e("composeAudio", "start compose bg");
            while (!firstAudioFinish || !secondAudioFinish) {
                index = 0;
                Log.e("composeAudio", "start compose bg" + index);
                firstAudioReadNumber = firstAudioInputStream.read(firstAudioByteBuffer);
                secondAudioReadNumber = secondAudioInputStream.read(secondAudioByteBuffer);

                int minAudioReadNumber = Math.min(firstAudioReadNumber, secondAudioReadNumber);
                int maxAudioReadNumber = Math.max(firstAudioReadNumber, secondAudioReadNumber);

                if (firstAudioReadNumber < 0) {
                    firstAudioFinish = true;
                }

                if (secondAudioReadNumber < 0) {
                    secondAudioFinish = true;
                }

                int halfMinAudioReadNumber = minAudioReadNumber / 2;

                outputShortArrayLength = maxAudioReadNumber / 2;

                for (; index < halfMinAudioReadNumber; index++) {
                    resultShort = CommonFunction.WeightShort(firstAudioByteBuffer[index * 2],
                            firstAudioByteBuffer[index * 2 + 1], secondAudioByteBuffer[index * 2],
                            secondAudioByteBuffer[index * 2 + 1], firstAudioWeight,
                            secondAudioWeight, Variable.isBigEnding);

                    outputShortArray[index] = resultShort;
                }

                if (firstAudioReadNumber != secondAudioReadNumber) {
                    if (firstAudioReadNumber > secondAudioReadNumber) {
                        for (; index < outputShortArrayLength; index++) {
                            resultShort = CommonFunction.GetShort(firstAudioByteBuffer[index * 2],
                                    firstAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                            outputShortArray[index] = (short) (resultShort * firstAudioWeight);
                        }
                    } else {
                        for (; index < outputShortArrayLength; index++) {
                            resultShort = CommonFunction.GetShort(secondAudioByteBuffer[index * 2],
                                    secondAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                            outputShortArray[index] = (short) (resultShort * secondAudioWeight);
                        }
                    }
                }

                if (outputShortArrayLength > 0) {
                    byte[] bytes = CommonFunction.GetByteBuffer(outputShortArray, Variable.isBigEnding);
                    composePcmOutputStream.write(bytes, 0, bytes.length);
                    byte[] offerEncoder = aacEncode.offerEncoder(bytes);
                    Log.e("offerEncoder", "offerEncoder Size:" + offerEncoder.length);
                    composeAACOutputStream.write(offerEncoder, 0, offerEncoder.length);

                    int encodedSize = LameUtil.encode(outputShortArray, outputShortArray,
                            outputShortArrayLength, mp3Buffer);

                    if (encodedSize > 0) {
                        composeAudioOutputStream.write(mp3Buffer, 0, encodedSize);
                    }
                }
            }
        } catch (Exception e) {
            LogFunction.error("ComposeAudio异常", e);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (composeAudioInterface != null) {
                        composeAudioInterface.composeFail();
                    }
                }
            });

            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (composeAudioInterface != null) {
                    composeAudioInterface.updateComposeProgress(50);
                }
            }
        });

        try {
            final int flushResult = LameUtil.flush(mp3Buffer);

            if (flushResult > 0) {
                composeAudioOutputStream.write(mp3Buffer, 0, flushResult);
            }
        } catch (Exception e) {
            LogFunction.error("释放ComposeAudio LameUtil异常", e);
        } finally {
            try {
                composeAudioOutputStream.close();
            } catch (Exception e) {
                LogFunction.error("关闭合成输出音频流异常", e);
            }

            LameUtil.close();
            aacEncode.close();
        }

        if (deleteSource) {
            FileFunction.DeleteFile(firstAudioFilePath);
            FileFunction.DeleteFile(secondAudioFilePath);
        }

        try {
            firstAudioInputStream.close();
            secondAudioInputStream.close();
            composePcmOutputStream.close();
            composeAACOutputStream.close();
        } catch (IOException e) {
            LogFunction.error("关闭合成输入音频流异常", e);
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (composeAudioInterface != null) {
                    composeAudioInterface.composeSuccess();
                }
            }
        });
    }

    public static void ComposeAudioList(List<RecordBean> recordBeanList, String secondAudioFilePath,
                                        String composeAudioFilePath, String composePcmUrl,
                                        float firstAudioWeight, float secondAudioWeight,
                                        final ComposeAudioInterface composeAudioInterface) {
        boolean secondAudioFinish = false;
        AACEncode aacEncode;
        byte[] firstAudioByteBuffer = new byte[byteBufferSize];
        byte[] secondAudioByteBuffer = new byte[byteBufferSize];
        byte[] mp3Buffer = new byte[(int) (7200 + (byteBufferSize * 1.25))];
        short[] outputShortArray = new short[byteBufferSize / 2];

        String aacUrl = Variable.StorageDirectoryPath + "hello.aac";

        Handler handler = new Handler(Looper.getMainLooper());

        List<FileInputStream> readFileInputStreams = new ArrayList<>();

        FileInputStream secondAudioInputStream = FileFunction.GetFileInputStreamFromFile
                (secondAudioFilePath);
        for (int i = 0; i < recordBeanList.size(); i++) {
            FileInputStream firstAudioInputStream = FileFunction.GetFileInputStreamFromFile
                    (recordBeanList.get(i).path);
            readFileInputStreams.add(firstAudioInputStream);
        }
        FileOutputStream composeAudioOutputStream = FileFunction.GetFileOutputStreamFromFile
                (composeAudioFilePath);
        FileOutputStream composePcmOutputStream = FileFunction.GetFileOutputStreamFromFile
                (composePcmUrl);
        FileOutputStream composeAACOutputStream = FileFunction.GetFileOutputStreamFromFile
                (aacUrl);

//        LameUtil.init(Constant.RecordSampleRate, Constant.LameBehaviorChannelNumber,
//                Constant.BehaviorSampleRate, Constant.LameBehaviorBitRate, Constant.LameMp3Quality);
        aacEncode = new AACEncode();

        int audioOffset;
        int currentOffset = 0;
        try {
            Log.e("composeAudio", "start compose recode");
            for (int i = 0; i < recordBeanList.size(); i++) {
                FileInputStream firstAudioInputStream = readFileInputStreams.get(i);
                RecordBean recordBean = recordBeanList.get(i);
                audioOffset = (int) (-1 * recordBean.start_time * Constant.RecordDataNumberInOneSecond) + currentOffset;
                Log.e("composeAudio", "audioOffset: " + audioOffset + " ---- currentOffset: " + currentOffset);
                if (i == recordBeanList.size() - 1) {
                    currentOffset = composeRecode(currentOffset, audioOffset, secondAudioFinish, handler, secondAudioInputStream, firstAudioByteBuffer, secondAudioByteBuffer,
                            composeAudioInterface, outputShortArray, secondAudioWeight, firstAudioWeight, firstAudioInputStream, composePcmOutputStream,
                            composeAACOutputStream, aacEncode, composeAudioOutputStream, mp3Buffer, true);
                } else {
                    currentOffset = composeRecode(currentOffset, audioOffset, secondAudioFinish, handler, secondAudioInputStream, firstAudioByteBuffer, secondAudioByteBuffer,
                            composeAudioInterface, outputShortArray, secondAudioWeight, firstAudioWeight, firstAudioInputStream, composePcmOutputStream,
                            composeAACOutputStream, aacEncode, composeAudioOutputStream, mp3Buffer, false);
                }
            }
        } catch (Exception e) {
            LogFunction.error("ComposeAudio异常", e);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (composeAudioInterface != null) {
                        composeAudioInterface.composeFail();
                    }
                }
            });

            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (composeAudioInterface != null) {
                    composeAudioInterface.updateComposeProgress(50);
                }
            }
        });

//        try {
//            final int flushResult = LameUtil.flush(mp3Buffer);
//
//            if (flushResult > 0) {
//                composeAudioOutputStream.write(mp3Buffer, 0, flushResult);
//            }
//        } catch (Exception e) {
//            LogFunction.error("释放ComposeAudio LameUtil异常", e);
//        } finally {
//            try {
//                composeAudioOutputStream.close();
//            } catch (Exception e) {
//                LogFunction.error("关闭合成输出音频流异常", e);
//            }
//
//            LameUtil.close();
//        }
        aacEncode.close();

        try {
            for (FileInputStream fileInputStream : readFileInputStreams) {
                fileInputStream.close();
            }
            composeAudioOutputStream.close();
            secondAudioInputStream.close();
            composePcmOutputStream.close();
            composeAACOutputStream.close();
        } catch (IOException e) {
            LogFunction.error("关闭合成输入音频流异常", e);
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (composeAudioInterface != null) {
                    composeAudioInterface.composeSuccess();
                }
            }
        });
    }

    private static int composeRecode(int currentOffset, int audioOffset, boolean secondAudioFinish, Handler handler,
                                     FileInputStream secondAudioInputStream, byte[] firstAudioByteBuffer, byte[] secondAudioByteBuffer,
                                     final ComposeAudioInterface composeAudioInterface, short[] outputShortArray, float secondAudioWeight, float firstAudioWeight,
                                     FileInputStream firstAudioInputStream, FileOutputStream composePcmOutputStream, FileOutputStream composeAACOutputStream,
                                     AACEncode aacEncode, final FileOutputStream composeAudioOutputStream, byte[] mp3Buffer, boolean isLast) {
        try {
            boolean firstAudioFinish = false;
            int firstAudioReadNumber;
            int secondAudioReadNumber;
            int index;
            int outputShortArrayLength;
            short resultShort;
            while (!firstAudioFinish && !secondAudioFinish) {
                index = 0;
                if (audioOffset < 0) {
                    Log.e("composeRecode", "0 > audioOffset : " + audioOffset);
                    secondAudioReadNumber = secondAudioInputStream.read(secondAudioByteBuffer);

                    outputShortArrayLength = secondAudioReadNumber / 2;

                    for (; index < outputShortArrayLength; index++) {
                        resultShort = CommonFunction.GetShort(secondAudioByteBuffer[index * 2],
                                secondAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                        outputShortArray[index] = (short) (resultShort * secondAudioWeight);
                    }

                    audioOffset += secondAudioReadNumber;
                    currentOffset += secondAudioReadNumber;

                    if (secondAudioReadNumber < 0) {
                        secondAudioFinish = true;
                        break;
                    }

                    if (audioOffset >= 0) {
                        break;
                    }
                } else {
                    Log.e("composeRecode", "0 <= audioOffset : " + audioOffset);
                    firstAudioReadNumber = firstAudioInputStream.read(firstAudioByteBuffer);

                    outputShortArrayLength = firstAudioReadNumber / 2;

                    for (; index < outputShortArrayLength; index++) {
                        resultShort = CommonFunction.GetShort(firstAudioByteBuffer[index * 2],
                                firstAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                        outputShortArray[index] = (short) (resultShort * firstAudioWeight);
                    }

                    audioOffset -= firstAudioReadNumber;
                    currentOffset -= firstAudioReadNumber;

                    if (firstAudioReadNumber < 0) {
                        firstAudioFinish = true;
                        break;
                    }

                    if (audioOffset <= 0) {
                        break;
                    }
                }

                if (outputShortArrayLength > 0) {

                    byte[] bytes = CommonFunction.GetByteBuffer(outputShortArray, Variable.isBigEnding);
//                    composePcmOutputStream.write(bytes, 0, bytes.length);
                    byte[] offerEncoder = aacEncode.offerEncoder(bytes);
                    composeAACOutputStream.write(offerEncoder, 0, offerEncoder.length);

//                    int encodedSize = LameUtil.encode(outputShortArray, outputShortArray,
//                            outputShortArrayLength, mp3Buffer);
//
//                    if (encodedSize > 0) {
//                        composeAudioOutputStream.write(mp3Buffer, 0, encodedSize);
//                    }
                }
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (composeAudioInterface != null) {
                        composeAudioInterface.updateComposeProgress(20);
                    }
                }
            });
            //合并背景音
            while (!firstAudioFinish || !secondAudioFinish) {
                index = 0;
                Log.e("composeRecode", "audioOffset : " + audioOffset);
                firstAudioReadNumber = firstAudioInputStream.read(firstAudioByteBuffer);
                secondAudioReadNumber = secondAudioInputStream.read(secondAudioByteBuffer);

                audioOffset += secondAudioReadNumber;
                currentOffset += secondAudioReadNumber;

                int minAudioReadNumber = Math.min(firstAudioReadNumber, secondAudioReadNumber);
                int maxAudioReadNumber = Math.max(firstAudioReadNumber, secondAudioReadNumber);

                if (firstAudioReadNumber < 0) {
                    firstAudioFinish = true;
                }

                if (secondAudioReadNumber < 0) {
                    secondAudioFinish = true;
                }

                int halfMinAudioReadNumber = minAudioReadNumber / 2;

                outputShortArrayLength = maxAudioReadNumber / 2;

                for (; index < halfMinAudioReadNumber; index++) {
                    resultShort = CommonFunction.WeightShort(firstAudioByteBuffer[index * 2],
                            firstAudioByteBuffer[index * 2 + 1], secondAudioByteBuffer[index * 2],
                            secondAudioByteBuffer[index * 2 + 1], firstAudioWeight,
                            secondAudioWeight, Variable.isBigEnding);

                    outputShortArray[index] = resultShort;
                }

                if (firstAudioReadNumber != secondAudioReadNumber) {
                    if (firstAudioReadNumber > secondAudioReadNumber) {
                        for (; index < outputShortArrayLength; index++) {
                            resultShort = CommonFunction.GetShort(firstAudioByteBuffer[index * 2],
                                    firstAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                            outputShortArray[index] = (short) (resultShort * firstAudioWeight);
                        }
                    } else {
                        for (; index < outputShortArrayLength; index++) {
                            resultShort = CommonFunction.GetShort(secondAudioByteBuffer[index * 2],
                                    secondAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                            outputShortArray[index] = (short) (resultShort * secondAudioWeight);
                        }
                    }
                }

                if (outputShortArrayLength > 0) {

                    byte[] bytes = CommonFunction.GetByteBuffer(outputShortArray, Variable.isBigEnding);
//                    composePcmOutputStream.write(bytes, 0, bytes.length);
                    byte[] offerEncoder = aacEncode.offerEncoder(bytes);
                    composeAACOutputStream.write(offerEncoder, 0, offerEncoder.length);

//                    int encodedSize = LameUtil.encode(outputShortArray, outputShortArray,
//                            outputShortArrayLength, mp3Buffer);
//
//                    if (encodedSize > 0) {
//                        composeAudioOutputStream.write(mp3Buffer, 0, encodedSize);
//                    }
                }

                //处理当前已经
                if (firstAudioFinish && !isLast) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currentOffset;
    }
}
