---
name: antigravity-python-ai
description: Python & AI 应用实战 - vLLM, DeepSeek, FastAPI, LangChain/LlamaIndex RAG, Docker 部署
---

# Python & AI 应用指南 (Antigravity Style)

## 使用场景
- 本地大模型部署与优化（vLLM, DeepSeek）
- 提示词工程（特别是银行等严谨场景）
- RAG 系统搭建（LangChain/LlamaIndex）
- PDF 数据提取、向量数据库集成
- Docker 容器化部署

## 核心技术栈

### 模型部署
- **vLLM**: 高性能推理引擎，支持 PagedAttention
- **DeepSeek**: 本地部署国产大模型
- **HuggingFace**: 模型下载与管理

### 应用框架
- **FastAPI**: 高性能异步 Web 框架
- **LangChain**: LLM 应用开发框架
- **LlamaIndex**: RAG 索引框架

### 向量数据库
- **Milvus**: 开源向量数据库
- **FAISS**: Facebook 向量检索库
- **Qdrant**: 轻量级向量数据库

## 环境配置

### Python 环境
```bash
# 优先使用 /opt/anaconda3/envs/base
conda activate base

# 验证 Python 版本
python --version  # 应显示 3.10+

# 创建虚拟环境
conda create -n ai-project python=3.10
conda activate ai-project

# 安装依赖
pip install -r requirements.txt
```

### 依赖管理 (requirements.txt)
```txt
# 核心框架
fastapi==0.109.0
uvicorn==0.27.0
pydantic==2.5.3

# AI 框架
langchain==0.1.8
langchain-community==0.0.20
llama-index==0.9.48

# 向量数据库
pymilvus==2.3.6
faiss-cpu==1.8.0

# 模型推理
vllm==0.4.0
transformers==4.37.2
accelerate==0.27.2

# 数据处理
pypdf==4.0.1
pandas==2.1.4
numpy==1.26.3

# 工具库
python-dotenv==1.0.0
httpx==0.26.0
tenacity==8.2.3
```

## vLLM 部署

### 1. 启动 vLLM 服务
```bash
# 使用 hf 别名下载模型
hf download --resume-download deepseek-ai/DeepSeek-V2

# 启动 vLLM OpenAI 兼容 API
python -m vllm.entrypoints.openai.api_server \
    --model deepseek-ai/DeepSeek-V2 \
    --host 0.0.0.0 \
    --port 8000 \
    --tensor-parallel-size 4 \
    --trust-remote-code

# 验证服务
curl http://localhost:8000/v1/models
```

### 2. vLLM 参数优化
```python
from vllm import LLM, SamplingParams

# 加载模型（优化显存）
llm = LLM(
    model="deepseek-ai/DeepSeek-V2",
    tensor_parallel_size=4,           # GPU 并行数
    trust_remote_code=True,           # 信任远程代码
    dtype="half",                     # 半精度浮点
    enforce_eager=True,               # 禁用 CUDA Graph
    max_model_len=4096,               # 最大上下文长度
    gpu_memory_utilization=0.9,       # GPU 显存利用率
)

# 采样参数
sampling_params = SamplingParams(
    temperature=0.7,                  # 温度（创造性）
    top_p=0.9,                        # Top-p 采样
    top_k=50,                         # Top-k 采样
    max_tokens=2048,                  # 最大输出长度
    stop=["<|im_end|>", "<|eot_id|>"],  # 停止符
)
```

## FastAPI 服务开发

### 1. 项目结构
```
project/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI 应用入口
│   ├── api/
│   │   ├── __init__.py
│   │   ├── router.py        # 路由定义
│   │   └── models.py        # 数据模型
│   ├── services/
│   │   ├── __init__.py
│   │   ├── llm_service.py   # LLM 服务
│   │   └── rag_service.py   # RAG 服务
│   └── utils/
│       ├── __init__.py
│       └── logger.py        # 日志工具
├── data/
│   ├── documents/           # 文档存储
│   └── vector_store/        # 向量索引存储
├── config.py                # 配置文件
├── requirements.txt
└── Dockerfile
```

### 2. FastAPI 应用模板
```python
# app/main.py
from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging

from app.api.router import api_router
from app.utils.logger import setup_logger

# 配置日志
logger = setup_logger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    logger.info("🚀 应用启动中...")
    # 初始化服务
    # await init_services()
    logger.info("✅ 应用启动完成")
    yield
    logger.info("👋 应用关闭中...")
    # 清理资源
    # await cleanup_services()

# 创建 FastAPI 应用
app = FastAPI(
    title="Antigravity AI API",
    description="基于 vLLM + RAG 的 AI 应用接口",
    version="1.0.0",
    lifespan=lifespan,
)

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(api_router, prefix="/api/v1")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        workers=1,
    )
```

### 3. 路由定义
```python
# app/api/router.py
from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
import logging

from app.services.llm_service import LLMService
from app.api.models import ChatRequest, ChatResponse

logger = logging.getLogger(__name__)
api_router = APIRouter()

# 依赖注入获取服务
def get_llm_service() -> LLMService:
    return LLMService()

@api_router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    service: LLMService = Depends(get_llm_service),
) -> ChatResponse:
    """
    聊天接口
    
    Args:
        request: 聊天请求
        service: LLM 服务实例
    
    Returns:
        ChatResponse: 聊天响应
    """
    try:
        logger.info(f"收到聊天请求: {request.messages[-1].content[:100]}...")
        
        # 调用 LLM 服务
        response = service.chat(
            messages=request.messages,
            temperature=request.temperature,
            max_tokens=request.max_tokens,
        )
        
        return ChatResponse(
            message=response["content"],
            usage=response["usage"],
        )
    except Exception as e:
        logger.error(f"聊天请求失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@api_router.post("/chat/rag")
async def chat_with_rag(
    request: ChatRequest,
    service: LLMService = Depends(get_llm_service),
) -> ChatResponse:
    """带 RAG 的聊天接口"""
    try:
        # 检索相关文档
        context = service.retrieve_context(request.message)
        
        # 构建提示词
        prompt = service.build_rag_prompt(request.message, context)
        
        # 生成回复
        response = service.generate(prompt)
        
        return ChatResponse(
            message=response,
            sources=context["sources"],
        )
    except Exception as e:
        logger.error(f"RAG 聊天失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))
```

### 4. 数据模型
```python
# app/api/models.py
from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
from enum import Enum

class Role(str, Enum):
    """消息角色"""
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"

class Message(BaseModel):
    """聊天消息"""
    role: Role
    content: str

class ChatRequest(BaseModel):
    """聊天请求"""
    messages: List[Message] = Field(..., description="消息列表")
    temperature: Optional[float] = Field(0.7, ge=0, le=2, description="温度参数")
    max_tokens: Optional[int] = Field(2048, ge=1, le=4096, description="最大输出长度")

class ChatResponse(BaseModel):
    """聊天响应"""
    message: str
    usage: Optional[dict] = None
    sources: Optional[List[str]] = None

class Document(BaseModel):
    """文档模型"""
    id: Optional[str] = None
    title: str
    content: str
    metadata: Optional[dict] = None
    created_at: Optional[datetime] = None
```

## RAG 系统搭建

### 1. 文档加载与处理
```python
# app/services/rag_service.py
from llama_index.core import VectorStoreIndex, Document, Settings
from llama_index.core.node_parser import SentenceSplitter
from llama_index.core import StorageContext, load_index_from_storage
from llama_index.core.retrievers import VectorIndexRetriever
from llama_index.core.query_engine import RetrieverQueryEngine
from llama_index.core.postprocessor import SimilarityPostprocessor
from langchain.text_splitter import RecursiveCharacterTextSplitter
import logging

logger = logging.getLogger(__name__)

class RAGService:
    def __init__(self, vector_store, model_name: str = "deepseek-ai/DeepSeek-V2"):
        self.vector_store = vector_store
        self.model_name = model_name
        self.index = None
        self.query_engine = None
        
        # 配置文本分割器
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=1000,           # 块大小
            chunk_overlap=200,          # 块重叠
            separators=["\n\n", "\n", "。", "！", "？", "；", " ", ""],
        )
    
    def load_documents(self, file_paths: List[str]):
        """
        加载文档
        
        Args:
            file_paths: 文件路径列表
        """
        from llama_index.core import SimpleDirectoryReader
        
        # 加载文档
        reader = SimpleDirectoryReader(input_files=file_paths)
        documents = reader.load_data()
        
        # 文本分割
        splitter = SentenceSplitter(
            chunk_size=1000,
            chunk_overlap=200,
        )
        
        # 创建索引
        self.index = VectorStoreIndex.from_documents(
            documents,
            transformations=[splitter],
            vector_store=self.vector_store,
        )
        
        # 构建查询引擎
        self.query_engine = self.index.as_query_engine(
            similarity_top_k=3,           # 返回 top-k 结果
            node_postprocessors=[
                SimilarityPostprocessor(similarity_cutoff=0.7)
            ],
        )
        
        logger.info(f"已加载 {len(documents)} 个文档，构建索引成功")
    
    def retrieve(self, query: str, top_k: int = 3) -> List[dict]:
        """
        检索相关文档
        
        Args:
            query: 查询文本
            top_k: 返回结果数量
        
        Returns:
            相关文档列表
        """
        if not self.query_engine:
            raise ValueError("请先加载文档，构建索引")
        
        # 检索
        retriever = VectorIndexRetriever(
            index=self.index,
            similarity_top_k=top_k,
        )
        
        results = retriever.retrieve(query)
        
        return [
            {
                "content": node.text,
                "score": node.score,
                "metadata": node.metadata,
            }
            for node in results
        ]
    
    def query(self, question: str) -> str:
        """
        基于文档回答问题
        
        Args:
            question: 问题
        
        Returns:
            回答内容
        """
        if not self.query_engine:
            raise ValueError("请先加载文档，构建索引")
        
        response = self.query_engine.query(question)
        return str(response)
    
    def persist(self, persist_dir: str):
        """持久化索引"""
        self.index.storage_context.persist(persist_dir)
        logger.info(f"索引已持久化到: {persist_dir}")
```

### 2. 向量数据库集成
```python
# app/services/vector_store.py
from llama_index.core.vector_stores import (
    VectorStore, 
    MetadataFilters, 
    ExactMatchFilter,
)
from llama_index.core.schema import Node, TextNode
from pymilvus import Collection, FieldSchema, CollectionSchema, DataType, MilvusClient
import logging

logger = logging.getLogger(__name__)

class MilvusVectorStore(VectorStore):
    """Milvus 向量存储"""
    
    def __init__(
        self,
        uri: str = "http://localhost:19530",
        collection_name: str = "documents",
        dim: int = 1024,
        overwrite: bool = True,
    ):
        self.client = MilvusClient(uri=uri)
        self.collection_name = collection_name
        self.dim = dim
        self.overwrite = overwrite
        self._collection = None
        
        # 初始化集合
        self._init_collection()
    
    def _init_collection(self):
        """初始化 Milvus 集合"""
        # 定义 schema
        schema = CollectionSchema(
            fields=[
                FieldSchema(name="id", dtype=DataType.VARCHAR, is_primary=True),
                FieldSchema(name="text", dtype=DataType.VARCHAR),
                FieldSchema(name="metadata", dtype=DataType.JSON),
                FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=self.dim),
            ],
            description="Document chunks vector store",
        )
        
        # 创建集合
        if self.client.has_collection(self.collection_name):
            if self.overwrite:
                self.client.drop_collection(self.collection_name)
                self.client.create_collection(schema, index_params=None)
        else:
            self.client.create_collection(schema)
        
        # 加载集合到内存
        self._collection = Collection(self.collection_name)
        
        # 创建索引
        index_params = {
            "metric_type": "COSINE",
            "index_type": "IVF_FLAT",
            "params": {"nlist": 1024},
        }
        self._collection.create_index("embedding", index_params)
        
        logger.info(f"Milvus 集合 {self.collection_name} 初始化完成")
    
    def add(self, nodes: List[Node]):
        """添加节点到向量存储"""
        if not nodes:
            return
        
        # 提取嵌入和元数据
        ids = []
        texts = []
        metadatas = []
        embeddings = []
        
        for node in nodes:
            ids.append(node.node_id)
            texts.append(node.text)
            metadatas.append(node.metadata or {})
            embeddings.append(node.embedding)
        
        # 插入数据
        self._collection.insert({
            "id": ids,
            "text": texts,
            "metadata": metadatas,
            "embedding": embeddings,
        })
        
        logger.info(f"已添加 {len(nodes)} 个节点到向量存储")
    
    def delete(self, ref_doc_id: str, **kwargs):
        """删除节点"""
        # 根据 ref_doc_id 删除
        self._collection.delete(expr=f'metadata["ref_doc_id"] == "{ref_doc_id}"')
    
    def query(self, query_embedding: List[float], top_k: int = 5, filters: MetadataFilters = None):
        """检索向量"""
        # 执行搜索
        results = self._collection.search(
            data=[query_embedding],
            anns_field="embedding",
            param={"metric_type": "COSINE", "params": {"nprobe": 16}},
            limit=top_k,
            expr=filters.expression if filters else None,
        )
        
        # 返回结果
        from llama_index.core.schema import NodeWithScore
        
        nodes = []
        for hits in results:
            for hit in hits:
                nodes.append(
                    NodeWithScore(
                        node=TextNode(
                            text=hit.entity.get("text"),
                            metadata=hit.entity.get("metadata", {}),
                        ),
                        score=hit.score,
                    )
                )
        
        return nodes
```

## 提示词工程

### 银行场景提示词模板
```python
# app/prompts/banking.py

# 系统提示词（严谨场景）
BANKING_SYSTEM_PROMPT = """你是一位专业的银行客服助手，需要：

1. **身份定位**: 
   - 你是XX银行的智能客服
   - 始终使用专业、礼貌的语气
   - 回答要准确、严谨、可执行

2. **回答原则**:
   - 提供的信息必须准确无误
   - 涉及金额、时间、利率等关键信息要精确
   - 复杂问题要分步骤说明
   - 必要时引导用户联系人工客服

3. **安全规范**:
   - 永远不要索要用户的密码、验证码
   - 不要诱导用户点击陌生链接
   - 涉及资金操作要提示风险

4. **回答格式**:
   - 使用清晰的段落和列表
   - 关键信息加粗标注
   - 提供操作步骤时编号清晰
   - 必要时提供客服热线

请根据用户问题，提供专业、准确的回答。"""

# 贷款咨询提示词
LOAN_CONSULT_PROMPT = """用户咨询贷款问题。

用户问题: {question}

请提供以下信息：
1. **贷款产品介绍**: 产品名称、适用人群
2. **申请条件**: 基本要求、资质要求
3. **利率说明**: 基准利率、浮动范围
4. **申请流程**: 步骤说明、所需材料
5. **注意事项**: 关键条款、常见问题

如果问题超出范围，请引导用户联系人工客服。"""

# RAG 检索提示词
RAG_SYSTEM_PROMPT = """你是一个专业的知识库问答助手。

请基于以下检索到的上下文信息回答用户问题。

## 上下文
{context}

## 用户问题
{question}

## 回答要求
1. 基于上下文信息回答，不要编造信息
2. 如果上下文没有相关信息，明确告知用户
3. 回答要引用具体的上下文来源
4. 保持专业、清晰的回答风格

请开始回答："""
```

## Docker 部署

### 1. Dockerfile
```dockerfile
# 使用官方 Python 镜像
FROM python:3.10-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PIP_NO_CACHE_DIR=1
ENV PIP_DISABLE_PIP_VERSION_CHECK=1

# 安装系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装 Python 依赖
RUN pip install -r requirements.txt

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 8000

# 启动命令
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 2. docker-compose.yml
```yaml
version: '3.8'

services:
  api:
    build: .
    ports:
      - "8000:8000"
    environment:
      - OPENAI_API_BASE=http://vllm:8000/v1
      - MODEL_NAME=deepseek-ai/DeepSeek-V2
    depends_on:
      - vllm
    volumes:
      - ./data:/app/data
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]

  vllm:
    image: vllm/vllm-openai:latest
    ports:
      - "8001:8000"
    command: >
      python -m vllm.entrypoints.openai.api_server
      --model deepseek-ai/DeepSeek-V2
      --host 0.0.0.0
      --port 8000
      --tensor-parallel-size 1
      --trust-remote-code
    volumes:
      - ./models:/root/.cache/huggingface
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]

  milvus:
    image: milvusdb/milvus:latest
    ports:
      - "19530:19530"
    volumes:
      - milvus_data:/var/lib/milvus
    environment:
      - ETCD_ENDPOINTS=etcd:2379
      - MINIO_ADDRESS=minio:9000

  etcd:
    image: quay.io/coreos/etcd:latest
    ports:
      - "2379:2379"
    environment:
      - ETCD_NAME=etcd

  minio:
    image: minio/minio:latest
    ports:
      - "9000:9000"
    command: minio server /data
    environment:
      - MINIO_ROOT_USER=admin
      - MINIO_ROOT_PASSWORD=password123

volumes:
  milvus_data:
```

### 3. 部署脚本
```bash
#!/bin/bash
# deploy.sh

set -e

echo "🚀 开始部署 Antigravity AI 服务..."

# 1. 停止旧服务
echo "🛑 停止旧服务..."
docker compose down

# 2. 构建新镜像
echo "🔨 构建 Docker 镜像..."
docker compose build --no-cache

# 3. 启动服务
echo "🚀 启动服务..."
docker compose up -d

# 4. 等待服务就绪
echo "⏳ 等待服务就绪..."
sleep 30

# 5. 检查健康状态
echo "🔍 检查服务健康状态..."
curl -f http://localhost:8000/health || exit 1

echo "✅ 部署完成！"
echo "📝 API 文档: http://localhost:8000/docs"
echo "🔗 健康检查: http://localhost:8000/health"
```

## 最佳实践

### 1. 错误重试机制
```python
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

@retry(
    stop=stop_after_attempt(3),
    wait=wait_exponential(multiplier=1, min=2, max=10),
    retry=retry_if_exception_type((ConnectionError, TimeoutError)),
)
async def call_llm_api(prompt: str) -> str:
    """调用 LLM API，带重试机制"""
    async with httpx.AsyncClient(timeout=120.0) as client:
        response = await client.post(
            "http://vllm:8000/v1/completions",
            json={
                "model": "deepseek-ai/DeepSeek-V2",
                "prompt": prompt,
                "max_tokens": 2048,
                "temperature": 0.7,
            },
        )
        response.raise_for_status()
        return response.json()["choices"][0]["text"]
```

### 2. 速率限制
```python
from fastapi import Request, RateLimitExceeded
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address

limiter = Limiter(key_func=get_remote_address)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

@api_router.post("/chat")
@limiter.limit("10/minute")  # 限制每分钟 10 次请求
async def chat(request: Request, ...):
    ...
```

### 3. 监控指标
```python
from prometheus_client import Counter, Histogram, generate_latest

# 请求计数
REQUEST_COUNT = Counter(
    'api_requests_total',
    'Total API Requests',
    ['method', 'endpoint', 'status_code']
)

# 请求延迟
REQUEST_LATENCY = Histogram(
    'api_request_duration_seconds',
    'API Request Latency',
    ['method', 'endpoint']
)

@app.middleware("http")
async def metrics_middleware(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    duration = time.time() - start_time
    
    REQUEST_COUNT.labels(
        method=request.method,
        endpoint=request.url.path,
        status_code=response.status_code,
    ).inc()
    
    REQUEST_LATENCY.labels(
        method=request.method,
        endpoint=request.url.path,
    ).observe(duration)
    
    return response

@app.get("/metrics")
async def metrics():
    """Prometheus 指标端点"""
    return Response(content=generate_latest(), media_type="text/plain")
```

## 常见问题与解决方案

### Q1: vLLM 显存不足？
**方案**:
- 减小 `max_model_len`
- 使用半精度 `dtype="half"`
- 减小 `gpu_memory_utilization`
- 使用量化模型（如 AWQ/GPTQ）

### Q2: RAG 检索质量差？
**方案**:
- 优化文本分割策略
- 调整 `similarity_top_k` 和 `similarity_cutoff`
- 使用更好的 Embedding 模型
- 增加元数据过滤

### Q3: FastAPI 响应慢？
**方案**:
- 使用异步视图函数
- 开启连接池
- 添加缓存层
- 使用 Gunicorn + Uvicorn Workers

### Q4: 模型输出重复？
**方案**:
- 调整 `temperature` 参数
- 增加 `top_k` 或 `top_p`
- 添加停止符
- 使用 `frequency_penalty` 和 `presence_penalty`
