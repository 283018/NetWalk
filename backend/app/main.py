from fastapi import FastAPI
from app.routes import router
from app.database import engine, Base
from app import models

Base.metadata.create_all(bind=engine)

app = FastAPI(title="NetWalk API")

app.include_router(router)