/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.pinchzoom.camerafilter.filter;

import android.content.Context;
import android.opengl.GLES20;

import cn.pinchzoom.camerafilter.MyGLUtils;
import cn.pinchzoom.camerafilter.R;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class YellowBlueFilter extends CameraFilter {
    private int program;

    public YellowBlueFilter(Context context) {
        super(context);

        // Build shaders
        program = MyGLUtils.buildProgram(context, R.raw.vertext, R.raw.yellow_blue);
    }

    @Override
    public void onDraw(int cameraTexId, int canvasWidth, int canvasHeight) {
        setupShaderInputs(program,
                new int[]{canvasWidth, canvasHeight},
                new int[]{cameraTexId},
                new int[][]{});
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public String getName() {
        return "สีพื้นเหลืองอักษรน้ำเงิน";
    }
}
