// 不显示
void trans_none(in vec4 texColor)
{
    discard;
}
// 透明度 [透明度]
vec4 trans_alpha(in vec4 texColor, in float alpha)
{
    return vec4(texColor.rgb, alpha);
}
// 圆形 [圆心横坐标，圆心纵坐标，半径，透明度]
vec4 trans_circle(in vec4 texColor, in float centerX, in float centerY, in float radius)
{
    if (distance(gl_FragCoord.xy, vec2(centerX, centerY)) < radius) {
        return texColor;
    }
    discard;
}
// 裁剪 [起点横坐标，终点横坐标，起点纵左边，终点纵左边]
vec4 trans_rect(in vec4 texColor, in float x1, in float x2, in float y1, in float y2)
{
    if (gl_FragCoord.x >= x1 && gl_FragCoord.x <= x2 &&
                gl_FragCoord.y >= y1 && gl_FragCoord.y <= y2) {
        return texColor;
    }
    discard;
}
// 横向百叶窗 [显示的起点横坐标，显示的起点纵坐标，分段长度，显示百分比]
vec4 trans_shutter(in vec4 texColor, in float startX, in float startY, in float segment, in float show)
{
    if (mod(floor(gl_FragCoord.x - startX), floor(segment)) > floor(show) ||
        mod(floor(gl_FragCoord.y - startY), floor(segment)) > floor(show)) {
        return texColor;
    }
    discard;
}
// 去掉低亮度 [ 阀值 ]
vec4 trans_bright(in vec4 texColor, in float threshold)
{
    float bright = .30 * texColor.r + .59 * texColor.g + .11 * texColor.b;
    if (bright >= threshold) {
        float alpha = 1.0;
        if (bright - threshold <= 0.2) {
            alpha = 5.0 * (bright - threshold);
        }
        return vec4(texColor.rgb, alpha);
    }
    discard;
}
// 丢掉多余的颜色 [红色，绿色，蓝色，透明度]
vec4 trans_threshold(in vec4 texColor, in float red, in float green, in float blue, in float alpha)
{
    if (texColor.r >= 0.5 && texColor.g >= 0.5 && texColor.b >= 0.5) {
        return vec4(red, green, blue, 1.0);
    }
    discard;
}

void trans_tex(vec4 texColor, in int type, in vec4 data)
{
    if (type == 0){
        trans_none(texColor);
    } else if (type == 1) {
        gl_FragColor = trans_alpha(texColor, data[0]);
    } else if (type == 2) {
        gl_FragColor = trans_circle(texColor, data[0], data[1], data[2]);
    } else if (type == 3) {
        gl_FragColor = trans_rect(texColor, data[0], data[1], data[2], data[3]);
    } else if (type == 4) {
        gl_FragColor = trans_shutter(texColor, data[0], data[1], data[2], data[3]);
    } else if (type == 5) {
        gl_FragColor = trans_bright(texColor, data[0]);
    } else if (type == 6) {
        gl_FragColor = trans_threshold(texColor, data[0], data[1], data[2], data[3]);
    }
}
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
//  黑白化
vec4 draw_gray(in vec2 texCoord)
{
    vec4 texColor = texture2D(sTexture, texCoord);
    float color = .30 * texColor.r + .59 * texColor.g + .11 * texColor.b;
    return vec4(color, color, color, texColor.a);
}
// 老照片
vec4 draw_old(in vec2 texCoord)
{
    vec4 texColor = texture2D(sTexture, texCoord);
    float red = 0.393*texColor.r+0.769*texColor.g+0.189*texColor.b;
    float green = 0.349*texColor.r+0.686*texColor.g+0.168*texColor.b;
    float blue = 0.272*texColor.r+0.534*texColor.g+0.131*texColor.b;
    return vec4(red, green, blue, texColor.a);
}
// 浮雕效果 [ 纹理的长度，纹理的高度]
vec4 draw_emboss(in vec2 texCoord, in float texWidth, in float texHeight)
{
    vec4 curColor = texture2D(sTexture, texCoord);
    vec4 upLeftColor = texture2D(sTexture, vec2((texCoord.x * texWidth - 1.0) / texWidth,
                (texCoord.y * texHeight -1.0) / texHeight));
    vec4 delColor = curColor - upLeftColor;
    float luminance = dot(delColor.rgb, vec3(0.3, 0.59, 0.11));
    return vec4(vec3(luminance), 0.0) + vec4(0.5, 0.5, 0.5, 1.0);
}
// 马赛克 [纹理的长度，纹理的高度，马赛克大小]
vec4 draw_mosaic(in vec2 texCoord, in float texWidth, in float texHeight, in float size)
{
    //当前点对应在纹理中的位置
    vec2 pointXY = vec2(texCoord.x * texWidth, texCoord.y * texHeight);
    //找到此点对应马赛克的起点
    vec2 mosaicXY = vec2(floor(pointXY.x / size) * size, floor(pointXY.y / size) * size);
    //转换坐标
    vec2 mosaicUV = vec2(mosaicXY.x / texWidth, mosaicXY.y / texHeight);
    return texture2D(sTexture, mosaicUV);
}
// 抖动 [ 颜色偏移距离]
vec4 draw_shake(in vec2 texCoord, in float offsetX, in float offsetY) {
    vec4 blue = texture2D(sTexture, texCoord);
    vec4 green = texture2D(sTexture, vec2(texCoord.x + offsetX, texCoord.y + offsetY));
    vec4 red = texture2D(sTexture, vec2(texCoord.x - offsetX, texCoord.y - offsetY));
    return vec4(red.x, green.y, blue.z, blue.w);
}
//随机函数
highp float nrand(in highp float x,in highp float y) {
    return fract(sin(dot(vec2(x, y), vec2(12.9898, 78.233))) * 43758.5453);
}
// 毛刺 [ 阀值，偏移距离，颜色偏移距离]
vec4 draw_burrs(in vec2 texCoord, in float threshold, in float offset) {
    float u = texCoord.x;
    float v = texCoord.y;
    highp float jitter = nrand(v, 0.0) * 2.0 - 1.0;//这里得到一个-1到1的数
    float offsetParam = step(threshold, abs(jitter));//step是gl自带函数，意思是:如果第一个参数大于第二个参数，那么返回0，否则返回1
    jitter = offsetParam * offset;//offsetParam 如果是0，就不偏移了，如果是1，就偏移 jitter * uDrawData[1] 的距离
    //这里计算最终的像素值，纹理坐标是0到1之间的数，如果小于0，那么图像就到屏幕右边去，如果超过1，那么就到屏幕左边去。
    vec4 color1 = texture2D(sTexture, vec2(u + jitter, v + jitter));
    vec4 color2 = texture2D(sTexture, vec2(u - jitter, v - jitter));
    return vec4(color1.r, color1.g, color2.b, 1.0);
}

vec4 get_tex_color(in vec2 texCoord, in int type, in vec4 data)
{
    if (type == 0) {
        return texture2D(sTexture, texCoord);
    } else if (type == 1) {
        return draw_gray(texCoord);
    } else if (type == 2) {
        return draw_old(texCoord);
    } else if (type == 3) {
        return draw_mosaic(texCoord, data[0], data[1], data[2]);
    } else if (type == 4) {
        return draw_emboss(texCoord, data[0], data[1]);
    } else if (type == 5) {
        return draw_shake(texCoord, data[0], data[1]);
    } else if (type == 6) {
        return draw_burrs(texCoord, data[0], data[1]);
    }
    return vec4(0.0, 0.0, 0.0, 0.0);
}

void main() {
    vec4 texColor = get_tex_color(vTextureCoordinate, uDrawType, uDrawData);
    trans_tex(texColor, uTransType, uTransData);
}