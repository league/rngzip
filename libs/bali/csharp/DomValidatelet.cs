using System;
using System.Text;
using System.Xml;
using System.Xml.Schema;
using org.relaxng.datatype;

namespace Org.Kohsuke.Bali {

	/// <summary>
	/// Object that validates DOM trees.
	/// </summary>
	public class DomValidatelet
	{
		private readonly Schema Schema;

		private State CurrentState;

		/// <summary>
		/// This object receives validation errors.
		/// </summary>
		public ValidationEventHandler EventHandler;

		public DomValidatelet( Schema schema ) {
			this.Schema = schema;
		}

		/// <summary>
		/// Validates a DOM document.
		/// </summary>
		/// <exception cref="DomValidationException">
		/// If the document is invalid
		/// </exception>
		public void Validate( XmlDocument document ) {
			Validate(document.DocumentElement);
		}

		/// <summary>
		/// Validates a DOM tree rooted at the specified element.
		/// </summary>
		/// <exception cref="DomValidationException">
		/// If the tree is invalid
		/// </exception>
		public void Validate( XmlElement e ) {
			CurrentState = Schema.InitialState;
			Visit(e);
		}

		private void Visit( XmlElement e ) {
			int name = Schema.GetNameCode( e.NamespaceURI, e.LocalName );
			AttributesSet atts = new AttributesSet();
			atts.Reset(Schema,e);
			CurrentState = CurrentState.StartElement( name, atts, State.emptySet );
			if( CurrentState==State.emptySet )
				throw new DomValidationException( e, "unexpected start element" );
			
			ValidationContext context = new XmlElementContext(e);

			StringBuilder builder = new StringBuilder();
			foreach( XmlNode n in e.ChildNodes ) {
				if( n is XmlText || n is XmlCDataSection || n is XmlWhitespace) {
					builder.Append( n.Value );
					continue;
				}
				if( n is XmlElement ) {
					ProcessText( builder, context, e, atts );
					Visit( (XmlElement)n );
				}
			}

			ProcessText( builder, context, e, atts );

			CurrentState = CurrentState.EndElement(atts, State.emptySet );
			if( CurrentState==State.emptySet )
				throw new DomValidationException( e, "unexpected end element" );
		}

		private void ProcessText( StringBuilder builder, ValidationContext context, XmlElement e, AttributesSet atts ) {
//			if( builder.Length==0 )
//				return;

			string text = builder.ToString();
			builder.Length = 0;

			CurrentState = CurrentState.Text( text, context, atts, State.emptySet );
			if( CurrentState==State.emptySet )
				throw new DomValidationException( e, "unexpected text" );
		}

	}

	/// <summary>
	/// ValidationContext implementation by using XmlElement
	/// </summary>
	internal sealed class XmlElementContext : ValidationContext {
		private readonly XmlElement element;

		internal XmlElementContext( XmlElement e ) {
			this.element=e;
		}

		public bool IsNotation( string str ) { return true; }
		public bool IsUnparsedEntity( string str ) { return true; }
		public string ResolveNamespacePrefix( string prefix ) {
			// TODO: revisit the exact condition
			return element.GetNamespaceOfPrefix(prefix);
		}
	}

	public class DomValidationException : Exception
	{
		/// <summary>
		/// The node that caused the error
		/// </summary>
		public readonly XmlElement SourceNode;
		
		public DomValidationException( XmlElement e, string message ) : base(message) {
			this.SourceNode = e;
		}
	}
}// end namespace
