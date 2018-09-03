# vertx3-learning

* 20180903 终于用vertx完成了类型nginx的功能。
 * 将受到的请求在读完header之后就将此请求转发给后端服务器。本示例中可以用作文件的上传下载，且已测。但是目前没有做过压力测试，仅仅是一个示例。只是证明了vertx是可以模拟类似的nginx功能的。
 * 在尝试的过程中出现了connection was closed异常，查询了好久。原因为没有调用httpclientrequest的end方法。具体看javadoc的描述和代码中的注释。
