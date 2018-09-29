package com.JUtils.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 文件分析
 * <p>
 * 1.对文件分块，交由不同线程处理。后置块需包含前置结尾
 * 2.用内存映射处理
 * 3.暴力匹配字符串
 */
public class FileAnalysis {

    private int threadNum;
    private String filePath;
    private ExecutorService executorService;
    private String dest = "";

    public void init(int threadNum, String filePath, String dest) {
        this.threadNum = threadNum;
        this.filePath = filePath;
        this.executorService = new ThreadPoolExecutor(threadNum, threadNum, 100, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        this.dest = dest;
    }

    public void start() {
        if (0 == threadNum || (filePath == null || filePath.isEmpty())) {
            System.out.println("数据初始化失败threadNum=" + threadNum + "filePath=" + filePath);
        }
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r");
             FileChannel fileChannel = file.getChannel()) {
            int count = 0;
            long size = fileChannel.size();
            long subSize = size / threadNum;
            List<Future<Integer>> list = new ArrayList<>();
            for (int i = 0; i < threadNum; i++) {
                long startIndex = i * subSize;
                if (size % threadNum > 0 && i == threadNum - 1) {
                    subSize += size % threadNum;
                }
                FileChannel subFileChannel = null;
                try (RandomAccessFile subFile = new RandomAccessFile(filePath, "r")) {
                    subFileChannel = subFile.getChannel();
                    Future<Integer> integerFuture = executorService.submit(new LocalFileReader(subFileChannel, 256, startIndex, startIndex + subSize, dest, next(dest)));
                    list.add(integerFuture);
                } catch (Exception e) {
                    throw e;
                }
            }
            for (Future<Integer> f :
                    list) {
                count += f.get();
            }
            System.out.println("count=" + count);
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
    }


    class LocalFileReader implements Callable<Integer> {
        private FileChannel fileChannel;
        private long startIndex;
        private long endIndex;
        private String dstr;
        private int bufferSize;
        private int[] next;

        public LocalFileReader(FileChannel fileChannel, int bufferSize, long startIndex, long endIndex, String dstr, int[] next) {
            this.bufferSize = bufferSize;
            this.fileChannel = fileChannel;
            this.endIndex = endIndex;
            this.dstr = dstr;
            if (startIndex >= dstr.getBytes().length) {
                this.startIndex = startIndex - dstr.getBytes().length + 1;
            }
            this.next = next;
            System.out.println(this.endIndex + "-" + this.startIndex);
        }


        @Override
        public Integer call() {
            return read();
        }

        private int read() {
            try {
                MappedByteBuffer buff = fileChannel.map(FileChannel.MapMode.READ_ONLY, startIndex, endIndex - startIndex);
                byte[] dst = new byte[bufferSize + dstr.getBytes().length];
                int count = 0;
                for (int offset = 0; offset < buff.capacity(); offset += bufferSize) {
                    int pre = 0;
                    int length = 0;
                    if (offset > 0) {
                        for (int preIndex = 0; preIndex < dstr.getBytes().length - 1; preIndex++) {
                            dst[preIndex] = buff.get(offset - dstr.getBytes().length + 1 + preIndex);
                            length++;
                        }
                        pre += dstr.getBytes().length - 1;
                    }
                    if (buff.capacity() - offset >= bufferSize) {
                        // 剩余文件大小大于等于bufferSize
                        for (int j = 0; j < bufferSize; j++) {
                            dst[j + pre] = buff.get(offset + j);
                            length++;
                        }
                    } else {
                        // 剩余文件大小小于bufferSize
                        for (int j = 0; j < buff.capacity() - offset; j++) {
                            dst[j + pre] = buff.get(offset + j);
                            length++;
                        }

                    }
                    count += countString(new String(dst, 0, length), dstr);
                }
                return count;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return 0;
        }

        /**
         * 暴力方法
         * @param str
         * @param s
         * @return
         */
//        private int countString(String str, String s) {
//            int count = 0;
//
//            while (str.contains(s)) {
//                str = str.substring(str.indexOf(s) + s.length(), str.length());
//                count++;
//            }
//            return count;
//        }

        /**
         * kmp
         *
         * @param str
         * @param s
         * @return
         */
        private int countString(String str, String s) {
            int count = 0;
            int index = -1;

            do {
                index = kmp(str, s, next);
                if (index != -1) {
                    str = str.substring(index + s.length());
                    count++;
                }
            } while (index != -1);
            return count;
        }

        private int kmp(String str, String dest, int[] next) {//str文本串  dest 模式串
            for (int i = 0, j = 0; i < str.length(); i++) {
                while (j > 0 && str.charAt(i) != dest.charAt(j)) {
                    j = next[j - 1];
                }
                if (str.charAt(i) == dest.charAt(j)) {
                    j++;
                }
                if (j == dest.length()) {
                    return i - j + 1;
                }
            }
            return -1;
        }
    }

    private int[] next(String dest) {
        int[] next = new int[dest.length()];
        next[0] = 0;
        for (int i = 1, j = 0; i < dest.length(); i++) {
            while (j > 0 && dest.charAt(j) != dest.charAt(i)) {
                j = next[j - 1];
            }
            if (dest.charAt(i) == dest.charAt(j)) {
                j++;
            }
            next[i] = j;
        }
        return next;
    }


    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        FileAnalysis fileAnalysis = new FileAnalysis();
        fileAnalysis.init(16, "E:\\workspace\\625.txt", "1234567890");
        fileAnalysis.start();
        System.out.println("接口耗时:" + (System.currentTimeMillis() - startTime) + "ms");
    }
}
