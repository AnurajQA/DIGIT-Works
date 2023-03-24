/**
 * import app from "./app";
import compression from "compression";
import helmet from "helmet";
import config from "./config";

app.use(helmet()); // set well-known security-related HTTP headers
app.use(compression());

app.disable("x-powered-by");

const port = config.app.port;
const server =  app.listen(port,()=>console.log(`Starting mukta services server on Port ${port} `));

export default server;
**/

import App from './app';
import PostsController from './controllers/posts/posts.controller';
 
const app = new App(
  [
    new PostsController(),
  ],
  8080,
);
 
app.listen();