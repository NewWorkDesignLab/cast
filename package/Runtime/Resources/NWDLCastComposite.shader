// Lays the virtual layer (premultiplied alpha, transparent where empty) over the camera image.
Shader "Hidden/NWDL/CastComposite"
{
    Properties
    {
        _MainTex ("Camera image", 2D) = "black" {}
        _VirtualTex ("Virtual layer", 2D) = "black" {}
    }
    SubShader
    {
        Cull Off ZWrite Off ZTest Always
        Pass
        {
            CGPROGRAM
            #pragma vertex vert_img
            #pragma fragment frag
            #include "UnityCG.cginc"

            sampler2D _MainTex;
            sampler2D _VirtualTex;

            fixed4 frag(v2f_img i) : SV_Target
            {
                fixed4 cam = tex2D(_MainTex, i.uv);
                fixed4 virt = tex2D(_VirtualTex, i.uv);
                return fixed4(virt.rgb + cam.rgb * (1 - virt.a), 1);
            }
            ENDCG
        }
    }
}
