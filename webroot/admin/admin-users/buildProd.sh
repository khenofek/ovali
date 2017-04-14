rm -rf dist/ tmp/
cp src/index.prod.html src/index.html
ng build -prod
