package syncUtils;

import java.io.Serializable;

public class SyncPacket implements Serializable {
    public static final int SLICE_SIZE = 1024 * 512;
    public static final String CHECK_SUM_ALGORITHM = "MD5";

    private String mFileName;
    private int mSliceNumber;
    private long mTotalLength;
    private byte[] mData;
    private String mCheckSum;

    public SyncPacket(String fileName, int sliceNumber, long totalLength, byte[] data, String checkSum){
        mFileName = fileName;
        mSliceNumber = sliceNumber; mTotalLength = totalLength;
        mData = data; mCheckSum = checkSum;
    }

    public String getFileName() {
        return mFileName;
    }

    public int getSliceNumber() {
        return mSliceNumber;
    }

    public double getTotalLength() {
        return mTotalLength;
    }

    public byte[] getData() {
        return mData;
    }

    public String getCheckSum() {
        return mCheckSum;
    }

    @Override
    public String toString() {
        return "FileName: " + mFileName +
                "; SliceNumber: " + mSliceNumber + "; TotalLength: " + mTotalLength + "; CheckSum: " + mCheckSum;
    }
}
