package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;

public class CRender {
    public final Renderer2D R2D;
    public final Renderer2D TR2D;
    public final Renderer3D R3D;

    public Mesh mesh = new ShaderMesh(Shaders.POS_COLOR,DrawMode.Triangles, Mesh.Attrib.Vec2, Mesh.Attrib.Color);

    public CRender() {
        R2D = new Renderer2D(false);
        TR2D = new Renderer2D(true);
        R3D = new Renderer3D();
    }

    public void begin(DrawMode mode) {
        this.mesh = new ShaderMesh(Shaders.POS_COLOR,mode, Mesh.Attrib.Vec2, Mesh.Attrib.Color);
        this.mesh.begin();
    }

    public void drawRect(double x, double y, double width, double height, Color color) {
        if (!mesh.isBuilding()) {
            begin(DrawMode.Triangles);
        }
        mesh.quad(
                mesh.vec2(x, y).color(color).next(),
                mesh.vec2(x, y + height).color(color).next(),
                mesh.vec2(x + width, y + height).color(color).next(),
                mesh.vec2(x + width, y).color(color).next()
        );
    }

    public void drawLine(double x, double y, double x2, double y2, Color color) {
        if (!mesh.isBuilding()) {
            begin(DrawMode.Lines);
        }
        mesh.line(
                mesh.vec2(x, y).color(color).next(),
                mesh.vec2(x2, y2).color(color).next()
        );
    }

    public void end(MatrixStack matrices) {
        this.mesh.end();
        this.mesh.render(matrices);
    }

    public void end() {
        this.end(null);
    }
}
