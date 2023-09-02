
from mounter.languages.java import JavaProject
import Utility.project as Utility

def manifest():
	return JavaProject(__file__,Utility)