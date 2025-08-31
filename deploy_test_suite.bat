echo Deploying test suite...
del gdhcntest.zip
7z a gdhcntest.zip .\gdhcntest\*
curl -F updateSpecification=true -F specification=9E4C3D39XEB9CX4A5EX8D58XD8E8338E171D -F testSuite=@gdhcntest.zip --header "ITB_API_KEY: 2E86828DXEDB9X4C5CX8D5DX5BF0A406DAB9" -X POST http://localhost:10003/api/rest/testsuite/deploy
