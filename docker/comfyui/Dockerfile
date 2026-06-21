# Minimal ComfyUI image for the Boteco pipeline's stage 3 (image generation).
# Builds ComfyUI from source so the model/output paths are explicit and stable.
FROM python:3.11-slim

# CPU wheels by default; override with a CUDA index (e.g. .../whl/cu121) for GPU.
ARG TORCH_INDEX_URL=https://download.pytorch.org/whl/cpu

RUN apt-get update \
 && apt-get install -y --no-install-recommends git \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
RUN git clone --depth 1 https://github.com/comfyanonymous/ComfyUI.git

WORKDIR /app/ComfyUI
# Install torch/vision/audio together from the same index so their ABIs match;
# ComfyUI's requirements.txt pulls torchaudio in, and a mismatched build fails
# to load its native extension.
RUN pip install --no-cache-dir torch torchvision torchaudio --index-url ${TORCH_INDEX_URL} \
 && pip install --no-cache-dir -r requirements.txt

EXPOSE 8188
# COMFY_ARGS is injected by docker-compose (e.g. "--cpu" for CPU-only hosts).
CMD ["sh", "-c", "python main.py --listen 0.0.0.0 --port 8188 ${COMFY_ARGS}"]
