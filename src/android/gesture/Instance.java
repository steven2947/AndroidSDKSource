/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.gesture;


/**
 * An instance represents a sample if the label is available or a query if the
 * label is null.
 * <p>
 * 如果label是可用的,那么instance代表一个实例,否则代表需要查询.
 * 实例
 */
class Instance {

    /**
     * 取样点的数量
     */
    private static final int SEQUENCE_SAMPLE_SIZE = 16;

    private static final int PATCH_SAMPLE_SIZE = 16;

    private final static float[] ORIENTATIONS = {
            0,
            (float) (Math.PI / 4),
            (float) (Math.PI / 2),
            (float) (Math.PI * 3 / 4),
            (float) Math.PI, -0,
            (float) (-Math.PI / 4),
            (float) (-Math.PI / 2),
            (float) (-Math.PI * 3 / 4),
            (float) -Math.PI
    };

    // the feature vector 向量
    final float[] vector;

    // the label can be null
    final String label;

    // the id of the instance
    final long id;

    private Instance(long id, float[] sample, String sampleName) {
        this.id = id;
        vector = sample;
        label = sampleName;
    }

    /**
     * 标准化数据
     */
    private void normalize() {
        float[] sample = vector;
        float sum = 0;

        int size = sample.length;
        for (int i = 0; i < size; i++) {
            sum += sample[i] * sample[i];//所有的坐标,包含x,y轴的平方和
        }

        float magnitude = (float) Math.sqrt(sum);//开平方根
        for (int i = 0; i < size; i++) {
            sample[i] /= magnitude;
        }
    }

    /**
     * create a learning instance for a single stroke gesture
     * 创建一个会学习的实例
     *
     * @param gesture
     * @param label
     * @return the instance
     */
    static Instance createInstance(int sequenceType, int orientationType, Gesture gesture, String label) {
        float[] pts;
        Instance instance;
        if (sequenceType == GestureStore.SEQUENCE_SENSITIVE) {//单笔手势

            //得到一个离散点的数组
            pts = temporalSampler(orientationType, gesture);
            instance = new Instance(gesture.getID(), pts, label);
            instance.normalize();
        } else {
            pts = spatialSampler(gesture);
            instance = new Instance(gesture.getID(), pts, label);
        }
        return instance;
    }

    //空间取样
    private static float[] spatialSampler(Gesture gesture) {
        return GestureUtils.spatialSampling(gesture, PATCH_SAMPLE_SIZE, false);
    }

    //时间取样
    private static float[] temporalSampler(int orientationType, Gesture gesture) {
        //离散点
        float[] pts = GestureUtils.temporalSampling(gesture.getStrokes().get(0), SEQUENCE_SAMPLE_SIZE);
        //重心点
        float[] center = GestureUtils.computeCentroid(pts);
        //计算弧度值(计算第一个点与重心点形成的角度的弧度值)
        float orientation = (float) Math.atan2(pts[1] - center[1], pts[0] - center[0]);

        //???调整
        float adjustment = -orientation;
        if (orientationType != GestureStore.ORIENTATION_INVARIANT) {
            int count = ORIENTATIONS.length;
            for (int i = 0; i < count; i++) {
                float delta = ORIENTATIONS[i] - orientation;
                if (Math.abs(delta) < Math.abs(adjustment)) {
                    adjustment = delta;
                }
            }
        }

        //根据中心点平移,平移到中心点在原点上
        GestureUtils.translate(pts, -center[0], -center[1]);
        //根据调整出来的adjustment旋转数据
        GestureUtils.rotate(pts, adjustment);

        return pts;
    }

}
