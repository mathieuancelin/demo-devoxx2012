Weld-OSGi : Devoxx 2012 Demo
===============

In order to build the project, you just have to clone the project 

```
$ git clone https://github.com/mathieuancelin/demo-devoxx2012.git demo
```

and compile it with maven 3

```
$ cd demo
$ mvn clean install
```

Then you just have to run the OSGi container with 

```
./run.sh
```

and you can visit the application at `http://localhost:9000/static/`