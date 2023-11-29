(ns renderer.attribute.hierarchy)

(defmulti update-attr (fn [_ attr] attr))
(defmulti description (fn [attr] attr))
(defmulti form-element (fn [k _v _disabled? _initial] k))

(defmethod update-attr :default
  [el attr f & more]
  (apply update-in el [:attrs attr] f more))

(defmethod description :d
  []
  "The d attribute defines a path to be drawn.")

(defmethod description :x
  []
  "The x attribute defines an x-axis coordinate in the user coordinate system.")

(defmethod description :y
  []
  "The y attribute defines a y-axis coordinate in the user coordinate system.")

(defmethod description :x1
  []
  "The x1 attribute is used to specify the first x-coordinate for drawing an 
   SVG element that requires more than one coordinate. Elements that only need 
   one coordinate use the x attribute instead.")

(defmethod description :y1
  []
  "The y1 attribute is used to specify the first y-coordinate for drawing an 
   SVG element that requires more than one coordinate. Elements that only need 
   one coordinate use the y attribute instead.")

(defmethod description :x2
  []
  "The x2 attribute is used to specify the second x-coordinate for drawing an 
   SVG element that requires more than one coordinate. Elements that only need 
   one coordinate use the x attribute instead.")

(defmethod description :y2
  []
  "The y2 attribute is used to specify the second y-coordinate for drawing an 
   SVG element that requires more than one coordinate. Elements that only need 
   one coordinate use the y attribute instead.")

(defmethod description :cx
  []
  "The cx attribute define the x-axis coordinate of a center point.")

(defmethod description :cy
  []
  "The cy attribute define the y-axis coordinate of a center point.")

(defmethod description
  :dx
  []
  "The dx attribute indicates a shift along the x-axis on the position of an 
   element or its content.")

(defmethod description :dy
  []
  "The dy attribute indicates a shift along the y-axis on the position of an 
   element or its content.")

(defmethod description :width
  []
  "The width attribute defines the horizontal length of an element in the user 
   coordinate system.")

(defmethod description :height
  []
  "The height attribute defines the vertical length of an element in the user 
   coordinate system.")

(defmethod description :rx
  []
  "The rx attribute defines a radius on the x-axis.")

(defmethod description :ry
  []
  "The ry attribute defines a radius on the y-axis.")

(defmethod description :r
  []
  "The r attribute defines the radius of a circle.")

(defmethod description :rotate
  []
  "The rotate attribute specifies how the animated element rotates as it travels 
   along a path specified in an <animateMotion> element.")

(defmethod description :stroke
  []
  "The stroke attribute is a presentation attribute defining the color 
   (or any SVG paint servers like gradients or patterns) used to paint 
   the outline of the shape;")

(defmethod description :fill
  []
  "The fill attribute has two different meanings. For shapes and text it's 
   a presentation attribute that defines the color (or any SVG paint servers 
   like gradients or patterns) used to paint the element; for animation 
   it defines the final state of the animation.")

(defmethod description :stroke-width
  []
  "The stroke-width attribute is a presentation attribute defining the width 
   of the stroke to be applied to the shape.")

(defmethod description :stroke-dasharray
  []
  "The stroke-dasharray attribute is a presentation attribute defining 
   the pattern of dashes and gaps used to paint the outline of the shape.")

(defmethod description :opacity
  []
  "The opacity attribute specifies the transparency of an object or of a group 
   of objects, that is, the degree to which the background behind the element 
   is overlaid.")

(defmethod description :id
  []
  "The id attribute assigns a unique name to an element.")

(defmethod description :class
  []
  "Assigns a class name or set of class names to an element. You may assign 
   the same class name or names to any number of elements, however, 
   multiple class names must be separated by whitespace characters.")

(defmethod description :tabindex
  []
  "The tabindex attribute allows you to control whether an element is focusable 
   and to define the relative order of the element for the purposes 
   of sequential focus navigation.")

(defmethod description :style
  []
  "The style attribute allows to style an element using CSS declarations. 
   It functions identically to the style attribute in HTML.")
