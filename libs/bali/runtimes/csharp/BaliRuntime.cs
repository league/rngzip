using System;
using System.Collections;
using System.Diagnostics;
using System.Xml;
using org.relaxng.datatype;
using org.relaxng.datatype.helpers;

namespace Org.Kohsuke.Bali {

	public class AttributesSet {
		private int count =0;
		
		private int[] names = new int[16];
		private string[] values = new string[16];

		private ValidationContext context;

		public AttributesSet() {}

		/** Re-initializes attributes. */
		public void Reset( Schema schema, XmlElement e ) {
			XmlAttributeCollection atts = e.Attributes;
			count = atts.Count;

			if( names.Length<count ) {
				// reallocate the buffer
				names = new int[count];
				values = new string[count];
			}

			int j=0;
			for( int i=0; i<count; i++ ) {
				XmlNode att = atts.Item(i);
				if( att.Name=="xmlns" || att.Name.StartsWith("xmlns:") )
					continue;
				names[j] = schema.GetNameCode( att.NamespaceURI, att.LocalName );
				values[j] = att.Value;
				j++;
			}

			count = j;

			this.context = new XmlElementContext(e);
		}


		public int Count { get { return count; } }
		public ValidationContext Context { get { return context; } }

		public int GetName( int idx ) { return names[idx]; }
		public string GetValue( int idx ) { return values[idx]; }


		/** Constant empty attributes. */
		public readonly static AttributesSet empty = new AttributesSet();
	}




	/// <summary>
	/// A state in the compiled automaton.
	/// </summary>
	public abstract class State {
	    
		/// <summary>
		/// Performs a transition by a start element event.
		/// </summary>
		public abstract State StartElement( int nameCode, AttributesSet attributes, State partialResult );
	    
		/**
		* Performs a transition by an end element event.
		* 
		* @param attributes
		*      attributes of the parent element of the element being closed.
		*/
		public abstract State EndElement( AttributesSet attributes, State partialResult );
	    
		/**
		* Expands StateSet by taking applicable attribute transitions.
		*/
		public abstract State Expand( AttributesSet attributes, State partialResult );
	    
		/**
		* Performs a transition by a text chunk.
		*/
		public abstract State Text( string value, ValidationContext context,
			AttributesSet attributes, State partialResult );
	    
		public virtual State WrapAfterByAfter( State newThen, State partialResult ) {
			Debug.Fail("can't happen for interleave/single state");
			throw new InvalidOperationException();
		}
		public virtual State WrapAfterByInterleaveLeft ( State lhs, Transition.Interleave alphabet, State partialResult ) {
			Debug.Fail("can't happen for interleave/single state");
			throw new InvalidOperationException();
		}
		public virtual State WrapAfterByInterleaveRight( State rhs, Transition.Interleave alphabet, State partialResult ) {
			Debug.Fail("can't happen for interleave/single state");
			throw new InvalidOperationException();
		}
	    
		/// <summary>
		/// Returns true if the current state is a final state.
		/// </summary>
		public abstract bool IsFinal { get; }

	    
		/**
		* Returns true if the choice tree contains the given state.
		*/
		public virtual bool Contains( State s ) {
			return this==s;
		}

		public override string ToString() {
			return ToString(0);
		}
		/**
		* @param parentPrecedence
		*      Operator precedence of the parent state.
		*      Bigger number means stronger precedence.
		*      1:AfterState, 2:ChoiceState, 3:InterleaveState
		*/
		public abstract string ToString( int parentPrecedence );
	    
		protected static string Parenthesis( string s ) {
			return '('+s+')';
		}
	    
	    
	    
		//
		//
		// factory methods
		//
		//
	    
		public static State MakeAfter( State child, State then ) {
			if(child==emptySet || then==emptySet)   return emptySet;
	        
			return new After(child,then);
		}

		public static State MakeChoice2( State block, State primitive ) {
			// this method allows the primitive to be of ChoiceState.
			if( primitive is Choice ) {
				Choice cs = (Choice)primitive;
	            
				return MakeChoice( MakeChoice2( block, cs.Lhs ), cs.Rhs );
			} else
				return MakeChoice( block, primitive );
		}
	    
		public static State MakeChoice( State block, State primitive ) {
			if(primitive is Choice) {
				Debug.Fail("primitive can't be a choice");
				throw new InvalidOperationException();
			}
	        
			if( block==emptySet )
				return primitive;
			if( primitive==emptySet )
				return block;
	        
			if( block.Contains(primitive) )
				return block;
	        
			return new Choice(block,primitive);
		}
	    
		public static State MakeInterleave( State lhs, State rhs, Transition.Interleave alphabet ) {
			if(lhs==emptySet || rhs==emptySet)  return emptySet;
	        
			return new Interleave(lhs,rhs,alphabet);
		}

	    
	    
		/** Singletone emptySet instance. */
		public static readonly State emptySet = new Empty();

	    
		internal class Empty : State {
			internal Empty() {}
	        
			public override State EndElement( AttributesSet attributes, State partialResult ) {
				return partialResult;
			}
			public override State Expand(AttributesSet attributes, State partialResult) {
				return partialResult;
			}
			public override bool IsFinal { get { return false; } }

			public override State StartElement( int nameCode, AttributesSet attributes, State partialResult ) {
				return partialResult;
			}
			public override State Text( string value, ValidationContext context, AttributesSet attributes, State partialResult) {
				return partialResult;
			}
			public override State WrapAfterByAfter( State newThen, State partialResult ) {
				return partialResult;
			}
			public override State WrapAfterByInterleaveLeft ( State lhs, Transition.Interleave alphabet, State partialResult ) {
				return partialResult;
			}
			public override State WrapAfterByInterleaveRight( State rhs, Transition.Interleave alphabet, State partialResult ) {
				return partialResult;
			}
			public override string ToString( int p ) { return "#err"; }
		}
	    
	    
	    
	    
	    
	    
	    
	    

	    
	    
	    
	    
	    
	    
	    
	    
		public class After : State {
			public readonly State Child;
			public readonly State Then;
	        
			public After( State c, State t ) {
				this.Child=c;
				this.Then=t;
			}

			public override State StartElement(
				int nameCode, AttributesSet attributes, State partialResult ) {
	            
				return Child.StartElement(
					nameCode,attributes,emptySet).WrapAfterByAfter( Then, partialResult );
			}
	    
			public override State EndElement( AttributesSet attributes, State partialResult ) {
				if( Child.IsFinal )		return Then.Expand(attributes,partialResult);
				else                    return partialResult;
			}
	    
			public override State Expand( AttributesSet attributes, State partialResult ) {
				return MakeChoice( partialResult,
					MakeAfter( Child.Expand(attributes,emptySet), Then ));
				// don't expand the "then" states because it needs different attributes.
				// we'll expand them at the endElement method.
			}
	        
			public override State Text( string value, ValidationContext context,
				AttributesSet attributes, State partialResult ) {
	                
				return MakeChoice( partialResult, MakeAfter(
					Child.Text(value,context,attributes,emptySet), Then ));
			}
	        
			public override State WrapAfterByAfter( State newThen, State partialResult ) {
				return MakeChoice( partialResult,
					MakeAfter( Child, MakeAfter( Then, newThen ) ));
			}
			public override State WrapAfterByInterleaveLeft( State lhs, Transition.Interleave alphabet, State partialResult ) {
				return MakeChoice( partialResult,
					MakeAfter( Child, MakeInterleave( lhs, Then, alphabet ) ) );
			}
			public override State WrapAfterByInterleaveRight( State rhs, Transition.Interleave alphabet, State partialResult ) {
				return MakeChoice( partialResult,
					MakeAfter( Child, MakeInterleave( Then, rhs, alphabet ) ) );
			}
	        
	        
	    
			public override bool Contains( State s ) {
				if(!(s is After)) return false;
	            
				After rhs = (After)s;
	            
				// TODO needs more generalization
				return this.Child.Contains(rhs.Child)
					&& rhs.Then.Contains(this.Then)
					&& this.Then.Contains(rhs.Then);
			}
	    
			public override bool IsFinal { get { return Child.IsFinal; } }
	    
	        
	        
			public override string ToString( int p ) {
				string s = Child.ToString(1)+" then "+Then.ToString(1);
				if( p>1 )   s = Parenthesis(s);
				return s;
			}
		}
	    
	    
	    
	    
		public class Choice : State {
			public readonly State Lhs,Rhs;
	        
			public Choice( State l, State r ) {
				this.Lhs=l;
				this.Rhs=r;
			}
	        
			public override State StartElement(
				int nameCode, AttributesSet attributes, State partialResult ) {
	            
				return Rhs.StartElement(nameCode,attributes,
					Lhs.StartElement(nameCode,attributes,partialResult));
			}
	        
			public override State EndElement( AttributesSet attributes, State partialResult ) {
				return Rhs.EndElement( attributes,
					Lhs.EndElement( attributes, partialResult ));
			}
	        
			public override State Expand( AttributesSet attributes, State partialResult ) {
				return Rhs.Expand( attributes, Lhs.Expand( attributes, partialResult ));
			}
	        
			public override State Text( string value, ValidationContext context,
				AttributesSet attributes, State partialResult ) {
	            
				return Rhs.Text(value,context,attributes,
					Lhs.Text(value,context,attributes,partialResult));
			}
	    
			public override State WrapAfterByAfter( State newThen, State partialResult ) {
				return Rhs.WrapAfterByAfter( newThen,
					Lhs.WrapAfterByAfter( newThen, partialResult ));
			}
			public override State WrapAfterByInterleaveLeft ( State newLhs, Transition.Interleave alphabet, State partialResult ) {
				return Rhs.WrapAfterByInterleaveLeft( newLhs, alphabet,
					Lhs.WrapAfterByInterleaveLeft( newLhs, alphabet, partialResult ));
			}
			public override State WrapAfterByInterleaveRight( State newRhs, Transition.Interleave alphabet, State partialResult ) {
				return Rhs.WrapAfterByInterleaveRight( newRhs, alphabet,
					Lhs.WrapAfterByInterleaveRight( newRhs, alphabet, partialResult ));
			}
	        
			public override bool Contains( State s ) {
				return Lhs.Contains(s) || Rhs.Contains(s);
			}
	        
			public override bool IsFinal { get { return Lhs.IsFinal || Rhs.IsFinal; } }
	    
	        
	    
			public override string ToString( int p ) {
				string s = Lhs.ToString(2)+"|"+Rhs.ToString(2);
				if( p>2 )   s = Parenthesis(s);
				return s;
			}
		}





		public class Interleave : State {
			public readonly State Lhs,Rhs;
			public readonly Transition.Interleave Alphabet;    // TODO: it might be better to extend it.
	        
			public Interleave( State l, State r, Transition.Interleave a ) {
				this.Lhs=l;
				this.Rhs=r;
				this.Alphabet=a;
			}
	    
			public override State StartElement(
				int nameCode, AttributesSet attributes, State result ) {
	            
				State l = Lhs.StartElement(nameCode,attributes,emptySet);
				State r = Rhs.StartElement(nameCode,attributes,emptySet);

				result = r.WrapAfterByInterleaveLeft ( Lhs, Alphabet, result );
				result = l.WrapAfterByInterleaveRight( Rhs, Alphabet, result );
	            
				return result;
			}
	    
			public override State EndElement( AttributesSet attributes, State partialResult ) {
				// this method can never be called because
				Debug.Fail("InterleaveState must not appear above AfterState.");
				throw new InvalidOperationException();
			}
	    
			public override State Expand( AttributesSet attributes, State partialResult ) {
				return MakeChoice( partialResult, MakeInterleave(
					Lhs.Expand(attributes,emptySet),
					Rhs.Expand(attributes,emptySet),
					Alphabet ));
			}
	        
			public override State Text( string value, ValidationContext context,
				AttributesSet attributes, State result ) {
	            
				State i;
	            
				if( Alphabet.TextToLeft )
					i = MakeInterleave( 
						Lhs.Text(value,context,attributes,emptySet),
						Rhs, Alphabet );
				else
					i = MakeInterleave(
						Lhs,
						Rhs.Text(value,context,attributes,emptySet),
						Alphabet );
	            
				result = MakeChoice( result, i );        
				if( i is Interleave && ((Interleave)i).CanJoin )
					result = Alphabet.Join.Expand( attributes, result );
	            
				return result;
			}
	        
			// two children must be joinable and the target state must be final
			public override bool IsFinal { get { return CanJoin && Alphabet.Join.IsFinal; } }
	        
			private bool CanJoin {
				get {
					return Lhs.IsFinal && Rhs.IsFinal;
				}
			}
	    
	    
			public override string ToString( int p ) {
				string s = Lhs.ToString(3)+"&"+Rhs.ToString(3)+"->#"+Alphabet.Join.Id;
				if( p>=2 )  s = Parenthesis(s);
				return s;
			}
		}
	    
	    
	    
	    
	    
		public class Single : State {
			public Single( bool _isFinal, bool _isPersistent, int _id ) {
				this.isFinal = _isFinal;
				this.IsPersistent = _isPersistent;
				this.Id = _id;
			}
	        
	        
	        
			public readonly int Id;    // id of this state. used for debugging only.
	        
			public readonly bool isFinal, IsPersistent;
	        
			// reference to the first transition. considered as immutable
			public Transition.Att aTr;
			public Transition.Data dTr;
			public Transition.Element eTr;
			public Transition.Interleave iTr;
			public Transition.List lTr;
			public Transition.NoAtt nTr;
			public Transition.Value vTr;



			public override State StartElement(
				int nameCode, AttributesSet attributes, State result ) {
	            
				for( Transition.Element e=eTr; e!=null; e=e.Next ) {
					if( e.Accepts(nameCode) ) {
						result = MakeChoice(result, MakeAfter(
							e.Left.Expand(attributes,emptySet),
							e.Right));    
					}
				}
	            
				return result;
			}
	        
			public override State EndElement( AttributesSet attributes, State partialResult ) {
				// this method can never be called because
				Debug.Fail("SingleState must not appear above AfterState");
				throw new InvalidOperationException();
			}
	        
	        
			public override State Text( string value, ValidationContext context,
				AttributesSet attributes, State result ) {
	            
				bool ignorable = (value.Trim().Length==0);
	            
				if(ignorable)   // this text is ignorable
					result = MakeChoice( result, this );
	            
				for( Transition.Data da=dTr; da!=null; da=da.Next ) {
	                
					// data transition can be taken when it's accepted by the datatype AND
					// the except expression fails.
					if( da.Datatype.IsValid(value,context)
					&&  !da.Left.Text( value, context, AttributesSet.empty, emptySet ).IsFinal )
						result = da.Right.Expand(attributes,result);
				}
	            
				for( Transition.Value va=vTr; va!=null; va=va.Next ) {
	                
					if( va.Accepts(value,context) )
						result = va.Right.Expand(attributes,result);
				}
	            
				for( Transition.List la=lTr; la!=null; la=la.Next ) {
	                
					string[] tokens = value.Split(' ','\t','\r','\n');
					// a list can't contain interleave, so no need to expand
					State child = la.Left;
	                
					foreach( string token in tokens ) {
						if( token.Length!=0 )
							child = child.Text( token, context, AttributesSet.empty, emptySet );
					}
	                
					if( child.IsFinal )
						result = la.Right.Expand(attributes,result);
				}
	            
				return result;
			}
	        
	    
	    
			public override State Expand( AttributesSet attributes, State result ) {
	            
				if( result.Contains(this) )   return result;  // no need to expand more
	            
				if( IsPersistent )
					result = MakeChoice( result, this );
	            
	            
				for( Transition.Att aa = aTr; aa!=null; aa=aa.Next ) {
					int matchCount=0,failCount=0;
	                
					for( int j=attributes.Count-1; j>=0; j-- ) {
	                    
						int nameCode = attributes.GetName(j);
	                    
						if( aa.Accepts(nameCode) ) {
							string value = attributes.GetValue(j);
	                        
							State s = aa.Left;
							s = s.Text( value, attributes.Context, AttributesSet.empty, emptySet );
	                        
							if( s.IsFinal )		matchCount++;
							else				failCount++;
						}
					}
	                
					if( (matchCount==1 && failCount==0 && !aa.Repeated)
					||  (matchCount!=0 && failCount==0 && aa.Repeated) )
						result = aa.Right.Expand( attributes, result );
				}
	            
				for( Transition.NoAtt nea = nTr; nea!=null; nea=nea.Next ) {
	                
					int j;
					for( j=attributes.Count-1; j>=0; j-- ) {
						int nc = attributes.GetName(j);
						if( nea.Accepts(nc) )
							// this attribute is prohibited.
							break;
					}
					if(j==-1)
						result = nea.Right.Expand( attributes, result );
				}
	            
				for( Transition.Interleave ia = iTr; ia!=null; ia=ia.Next ) {
					result = MakeChoice( result, MakeInterleave(
						ia.Left. Expand( attributes, emptySet ),
						ia.Right.Expand( attributes, emptySet ),
						ia ));
				}
	            
				return result;
			}
	    
	        
	        
			public override bool Contains( State s ) {
				return this==s;
			}
	    
			public override bool IsFinal { get { return isFinal; } }
	        
	        
			public override string ToString( int p ) {
				if( aTr==null && dTr==null && eTr==null && iTr==null && lTr==null && nTr==null && vTr==null && isFinal )
					return "#eps";
				return "#"+Id;
			}
		}
	}


	namespace Transition {
		
		/** Transition by attribute. */
		public sealed class Att {
			public Att( int mask, int test, bool repeated, State.Single left, State.Single right, Att next ) {
				this.Mask = mask;
				this.Test = test;
				this.Repeated = repeated;
				this.Left = left;
				this.Right = right;
				this.Next = next;
			}
			/** Name signature. */
			private readonly int Mask,Test;
			/** True if this alphabet is of the form @X+. */
			public readonly bool Repeated;
			/** Left and right states. */
			public readonly State.Single Left,Right;
		    
			/** Next transition of this kind, or null. */
			public readonly Att Next;
		    
			/** Returns true if this alphabet accets the given name code. */
			public bool Accepts( int nameCode ) {
				return (nameCode&Mask)==Test;
			}
		}
		
		/** Transition by data. */
		public class Data {
			public Data( Datatype dt, State.Single left, State.Single right, Data next ) {
				this.Datatype=dt;
				this.Left=left;
				this.Right=right;
				this.Next=next;
			}
			/** Datatype object to validate. */
			public readonly Datatype Datatype;
			/** Left and right states. */
			public readonly State.Single Left,Right;
		    
			/** Next transition of this kind, or null. */
			public readonly Data Next;
		}
		
		/** Transition by attribute. */
		public sealed class Element {
			public Element( int mask, int test, State.Single left, State.Single right, Element next ) {
				this.Mask = mask;
				this.Test = test;
				this.Left = left;
				this.Right = right;
				this.Next = next;
			}
			/** Name signature. */
			private readonly int Mask,Test;
			/** Left and right states. */
			public readonly State.Single Left,Right;
		    
			/** Next transition of this kind, or null. */
			public readonly Element Next;
		    
			/** Returns true if this alphabet accets the given name code. */
			public bool Accepts( int nameCode ) {
				return (nameCode&Mask)==Test;
			}
		}
		
		/** Transition by interleave. */
		public sealed class Interleave {
			public Interleave( State.Single left, State.Single right, State.Single join, bool textToLeft, Interleave next ) {
				this.Left = left;
				this.Right = right;
				this.Join = join;
				this.Next = next;
				this.TextToLeft = textToLeft;
			}
			/** Left and right states. */
			public readonly State.Single Left,Right;
			/** Join state. */
			public readonly State.Single Join;
		    
			/** True if text should be consumed by the left sub-automaton. */
			public readonly bool TextToLeft;
		    
			/** Next transition of this kind, or null. */
			public readonly Interleave Next;
		}

		/** Transition by list. */
		public sealed class List {
			public List( State.Single left, State.Single right, List next ) {
				this.Left = left;
				this.Right = right;
				this.Next = next;
			}
			/** Left and right states. */
			public readonly State.Single Left,Right;
		    
			/** Next transition of this kind, or null. */
			public readonly List Next;
		}

		/** Transition by non-existent attribute. */
		public sealed class NoAtt {
			public NoAtt( State.Single right, int[] negTests, int[] posTests, NoAtt next ) {
				this.Right = right;
				this.NegTests = negTests;
				this.PosTests = posTests;
				this.Next = next;
			}
			/** Right state. */
			public readonly State.Single Right;
		    
			/** Name tests. repeated (mask,test) pairs. */
			private readonly int[] NegTests, PosTests;
		    
			/** Next transition of this kind, or null. */
			public readonly NoAtt Next;
		    
			public bool Accepts( int code ) {
				for( int i=PosTests.Length-2; i>=0; i-=2 )
					if( (code&PosTests[i])==PosTests[i+1] )
						return false;
		        
				for( int i=NegTests.Length-2; i>=0; i-=2 )
					if( (code&NegTests[i])==NegTests[i+1] )
						return true;
		        
				return false;
			}
		}
		
		/** Transition by value. */
		public class Value {
			public Value( Datatype dt, object value, State.Single right, Value next ) {
				this.Datatype=dt;
				this.Obj = value;
				this.Right=right;
				this.Next=next;
			}
			/** Datatype object to validate. */
			private readonly Datatype Datatype;
			/** Value object to compare with. */
			private readonly object Obj;
		    
			public bool Accepts( string text, ValidationContext context ) {
				object o = Datatype.CreateValue(text,context);
				if(o==null)     return false;
				return Datatype.SameValue(o,Obj);
			}
		    
			/** Left and right states. */
			public readonly State.Single Right;
		    
			/** Next transition of this kind, or null. */
			public readonly Value Next;
		}
	}

	public class Schema {
	//
	//
	// name look-up
	//
	//
		/**
		* Dictionary for looking up name codes from (uri,local).
		* A map from StringPair to Integer.
		*/
		private readonly Hashtable nameLiterals = new Hashtable();
	    
		/** Default name code if a name is not found in the dictionary. */
		private readonly int defaultNameCode;
	    
		/**
		* (URI,local name) pair. In princile this should be immutable,
		* but to avoid excessive object creation this object is made mutable.
		*/
		private struct StringPair {
			internal StringPair( string uri, string local ) {
				this.uri=uri;
				this.local=local;
			}
			internal void set( string uri, string local ) {
				this.uri=uri;
				this.local=local;
			}
			private string uri,local;
			public override bool Equals( object o ) {
				StringPair rhs = (StringPair)o;
				return this.uri.Equals(rhs.uri) && this.local.Equals(rhs.local);
			}
			public override int GetHashCode() {
				return uri.GetHashCode()^local.GetHashCode();
			}
		}
	    
		/**
		* Looks up a name code from an (uri,local) pair.
		*/
		public int GetNameCode( string uri, string local ) {
			StringPair p = new StringPair(uri,local);
			object o;
	        
			o = nameLiterals[p];
			if(o!=null)     return (int)o;
	        
			p.set(uri,WILDCARD);
			o = nameLiterals[p];
			if(o!=null)     return (int)o;
	        
			return defaultNameCode;
		}
	    
		private const string WILDCARD = "*";





		/** Initial state. */
		public readonly State.Single InitialState;
	    
//		public Validatelet CreateValidatelet() {
//			return new ValidateletImpl(this);
//		}
	    
	//
	//
	// decoding
	//
	//
		private const int sizeOfState = 9;
		private const int sizeOfATr = 7; /* left(1),right(1),name(4),flag(1) */
		private const int sizeOfDTr = 3; /* left(1),right(1),datatype(1) */
		private const int sizeOfETr = 6; /* left(1),right(1),name(4) */
		private const int sizeOfITr = 4; /* left(1),right(1),join(1),flag(1) */
		private const int sizeOfLTr = 2; /* left(1),right(1) */
		// NTr is of variable length. The format is
		// right(1), negLen|posLen(1), name(4*negLen), name(4*posLen)
		private const int sizeOfVTr = 3; /* right(1),datatype(1),value(1) */


		public Schema(
			string nameLiterals,
			int defaultNameCode,
			string encStates,
			string encATr,
			string encDTr,
			string encETr,
			string encITr,
			string encLTr,
			string encNTr,
			string encVTr,
			object[] encDatatypes,
			object[] values,
			DatatypeLibraryFactory datatypeFactory ) :
	        
			this( nameLiterals, defaultNameCode, encStates,
				encATr, encDTr, encETr, encITr, encLTr, encNTr, encVTr,
				createDatatypes( datatypeFactory, encDatatypes ),
				values )
		{
		}
	    
		/**
		* @param nameLiterals
		*      Name literals encoded into a string as
		*      "[stateId1][uri1]\u0000[local1]\u0000[stateid2][uri2]\u0000 ..."
		* @param defaultNameCode
		*      The name code assigned to literals that are not described in the above
		*      dictionary.
		* @param encStates
		*      encoded state information.
		* @param encATr, encDTr, encETr, encITr, encLTr, encNTr, encVTr
		*      encoded transition tables (per alphabet type.)
		* @param datatypes
		*      Datatype objects used in this schema.
		* @param values
		*      Values used by &lt;value/> patterns.
		*/
		public Schema(
			string nameLiterals,
			int defaultNameCode,
			string encStates,
			string encATr,
			string encDTr,
			string encETr,
			string encITr,
			string encLTr,
			string encNTr,
			string encVTr,
			Datatype[] datatypes,
			object[] values ) {
	        
			// decode name literals
			while(nameLiterals.Length!=0) {
				int code = decodeInt(nameLiterals,0);   // name code
				nameLiterals = nameLiterals.Substring(2);
	            
				int idx;
	            
				idx = nameLiterals.IndexOf('\u0000');
				string uri = nameLiterals.Substring(0,idx);
				nameLiterals = nameLiterals.Substring(idx+1);
	            
				idx = nameLiterals.IndexOf('\u0000');
				string local = nameLiterals.Substring(0,idx);
				nameLiterals = nameLiterals.Substring(idx+1);
	            
				this.nameLiterals.Add( new StringPair(uri,local), code );
			}
	        
			this.defaultNameCode = defaultNameCode;
	        
			{// decode state and transition table
				string es;
				State.Single[] states = new State.Single[ encStates.Length/sizeOfState ];
	            
				// build basic state objects
				es=encStates;            
				for( int idx=0; es.Length!=0; idx++,es=es.Substring(sizeOfState) ) {
					char bitFlags = es[0];
					states[idx] = new State.Single( (bitFlags&2)!=0, (bitFlags&1)!=0, idx );
				}
	            
				bool[] decoded = new bool[states.Length];
	            
				// bare stack.
				int[] stack = new int[16];
				int stackPtr=0;
	            
				// decode transition table
				for( int idx=states.Length-1; idx>=0; idx-- ) {
	                
					int s = idx;
					while( s!=65535 && !decoded[s] ) {
						// this state needs to be decoded -- push this state
						if( stack.Length==stackPtr ) {
							// extend the stack
							int[] newBuf = new int[stack.Length*2];
							stack.CopyTo( newBuf,0 );
							stack = newBuf;
						}
						stack[stackPtr++] = s;
						decoded[s] = true;
	                    
						// decode next state
						s = encStates[ s*sizeOfState+1 ];
					}
	                
					while( stackPtr!=0 ) {
						// decode transitions from state 's'.
						s = stack[--stackPtr];
						State.Single current = states[s];
	                    
						// next state
						int nextStateIdx = encStates[ s*sizeOfState+1 ];
						State.Single nextState = (nextStateIdx==65535)?null:states[nextStateIdx];
	                    
						// decode transitions
	                    
						current.aTr = decodeATr( encStates, encATr, states, s, nextState==null?null:nextState.aTr );
						current.dTr = decodeDTr( encStates, encDTr, states, s, nextState==null?null:nextState.dTr, datatypes );
						current.eTr = decodeETr( encStates, encETr, states, s, nextState==null?null:nextState.eTr );
						current.iTr = decodeITr( encStates, encITr, states, s, nextState==null?null:nextState.iTr );
						current.lTr = decodeLTr( encStates, encLTr, states, s, nextState==null?null:nextState.lTr );
						current.nTr = decodeNTr( encStates, encNTr, states, s, nextState==null?null:nextState.nTr );
						current.vTr = decodeVTr( encStates, encVTr, states, s, nextState==null?null:nextState.vTr, datatypes, values );
					}
				}
	            
				InitialState = states[0];
			}
		}

		/** Decodes attribute transitions. */
		private static Transition.Att decodeATr( string encStates, string encATr, State.Single[] states, int s, Transition.Att next ) {
			int start = encStates[ s*sizeOfState+2 ];
			int end = (s!=states.Length-1)?encStates[ (s+1)*sizeOfState+2 ]:encATr.Length;
	        
			for( int i=end-sizeOfATr; i>=start; i-=sizeOfATr ) {
				next = new Transition.Att(
					decodeInt(encATr, i+2),
					decodeInt(encATr, i+4),
					encATr[i+6]=='R',
					states[encATr[i+0]],
					states[encATr[i+1]],
					next );
			}
	        
			return next;
		}

		/** Decodes data transitions. */
		private static Transition.Data decodeDTr( string encStates, string encDTr, State.Single[] states, int s, Transition.Data next, Datatype[] datatypes ) {
			int start = encStates[ s*sizeOfState+3 ];
			int end = (s!=states.Length-1)?encStates[ (s+1)*sizeOfState+3 ]:encDTr.Length;
	        
			for( int i=end-sizeOfDTr; i>=start; i-=sizeOfDTr ) {
				next = new Transition.Data(
					datatypes[ encDTr[ i+2 ] ],
					states[encDTr[i+0]],
					states[encDTr[i+1]],
					next );
			}
	        
			return next;
		}

		/** Decodes element transitions. */
		private static Transition.Element decodeETr( string encStates, string encETr, State.Single[] states, int s, Transition.Element next ) {
			int start = encStates[ s*sizeOfState+4 ];
			int end = (s!=states.Length-1)?encStates[ (s+1)*sizeOfState+4 ]:encETr.Length;
	        
			for( int i=end-sizeOfETr; i>=start; i-=sizeOfETr ) {
				next = new Transition.Element(
					decodeInt(encETr, i+2),
					decodeInt(encETr, i+4),
					states[encETr[i+0]],
					states[encETr[i+1]],
					next );
			}
	        
			return next;
		}
	                    
		/** Decodes interleave transitions. */
		private static Transition.Interleave decodeITr( string encStates, string encITr, State.Single[] states, int s, Transition.Interleave next ) {
			int start = encStates[ s*sizeOfState+5 ];
			int end = (s!=states.Length-1)?encStates[ (s+1)*sizeOfState+5 ]:encITr.Length;
	        
			for( int i=end-sizeOfITr; i>=start; i-=sizeOfITr ) {
				next = new Transition.Interleave(
					states[encITr[i+0]],
					states[encITr[i+1]],
					states[encITr[i+2]],
					encITr[i+3]=='L',
					next );
			}
	        
			return next;
		}
	                    
	                    
		/** Decodes list transitions. */
		private static Transition.List decodeLTr( string encStates, string encLTr, State.Single[] states, int s, Transition.List next ) {
			int start = encStates[ s*sizeOfState+6 ];
			int end = (s!=states.Length-1)?encStates[ (s+1)*sizeOfState+6 ]:encLTr.Length;
	        
			for( int i=end-sizeOfLTr; i>=start; i-=sizeOfLTr ) {
				next = new Transition.List(
					states[encLTr[i+0]],
					states[encLTr[i+1]],
					next );
			}
	        
			return next;
		}

		/** Decodes non-existent attribute transitions. */
		private static Transition.NoAtt decodeNTr( string encStates, string encNTr, State.Single[] states, int s, Transition.NoAtt next ) {
			int start = encStates[ s*sizeOfState+7 ];
			int end = (s!=states.Length-1)?encStates[ (s+1)*sizeOfState+7 ]:encNTr.Length;
	        
			for( int i=start; i<end; ) {
				State.Single right = states[encNTr[i+0]];
				char sz = encNTr[i+1];
				int szNeg = (sz>>8);
				int szPos = (sz&0xFF);
	            
				int[] negTest = new int[szNeg*2];
				int[] posTest = new int[szPos*2];
	            
				i+=2;
	            
				for( int j=0; szNeg>0; szNeg--,j+=2,i+=4 ) {
					negTest[j+0] = decodeInt(encNTr,i+0);
					negTest[j+1] = decodeInt(encNTr,i+2);
				}
				for( int j=0; szPos>0; szPos--,j+=2,i+=4 ) {
					posTest[j+0] = decodeInt(encNTr,i+0);
					posTest[j+1] = decodeInt(encNTr,i+2);
				}
	            
				next = new Transition.NoAtt(right,negTest,posTest,next);
			}
	        
			return next;
		}

	                    
		/** Decodes value transitions. */
		private static Transition.Value decodeVTr( string encStates, string encVTr, State.Single[] states, int s, Transition.Value next, Datatype[] datatypes, object[] values ) {
			int start = encStates[ s*sizeOfState+8 ];
			int end = (s!=states.Length-1)?encStates[ (s+1)*sizeOfState+8 ]:encVTr.Length;
	        
			for( int i=end-sizeOfVTr; i>=start; i-=sizeOfVTr ) {
				Datatype dt = datatypes[ encVTr[i+1] ];
	            int vidx = encVTr[i+2]*2;
				next = new Transition.Value(
					dt,
					dt.CreateValue( (string)values[vidx], createContext((string[])values[vidx+1]) ),
					states[encVTr[i+0]],
					next );
			}
	        
			return next;
		}

		private static ValidationContext createContext( string[] map ) {
			if(map==null)   return null;
			
			return new ValidationContextImpl(map);
		}

		private class ValidationContextImpl : ValidationContext {
			
			private readonly string[] map;

			internal ValidationContextImpl( string[] m ) {
				map=m;
			}

			public String ResolveNamespacePrefix(String prefix) {
				for( int i=0; i<map.Length/2; i++ )
					if( map[i].Equals(prefix) )
						return map[i+1];
				return null;
			}
			public String GetBaseUri() {
				return null;
			}
			public bool IsUnparsedEntity(String value) {
				return true;
			}
			public bool IsNotation(String value) {
				return true;
			}
		};



	    




	    
	    
	  
	    
	//
	//
	// decode utility methods
	//
	//
		private static int decodeInt( string s, int idx ) {
			return (((int)s[idx+1])<<16) | ((int)s[idx+0]);
		}
	    
		private static Datatype[] createDatatypes( DatatypeLibraryFactory factory, object[] encodedDatatypes ) {
			Datatype[] datatypes = new Datatype[encodedDatatypes.Length/3];
			for( int i=0; i<datatypes.Length; i++ ) {
				datatypes[i] = createDatatype( factory,
					(string)encodedDatatypes[i*3],
					(string)encodedDatatypes[i*3+1],
					(object[])encodedDatatypes[i*3+2] );
			}
			return datatypes;
		}
	    
		private static Datatype createDatatype( DatatypeLibraryFactory factory,
			string nsUri, string localName, object[] parameters ) {
	        
			if( nsUri.Length==0 ) {
				// since those parameters were compiled, we don't need to check the error.
				if( localName[0]=='t' )      return TokenType.theInstance;
				else                         return StringType.theInstance;
			}
	        
			DatatypeLibrary lib = factory.CreateDatatypeLibrary(nsUri);
			if(lib==null)
				throw new DatatypeException("unable to locate a datatype library for "+nsUri);
	        
			DatatypeBuilder builder = lib.CreateDatatypeBuilder(localName);
	        
            for( int i=0; i<parameters.Length; i+=3 ) {
                ValidationContext context = createContext( (string[])parameters[i+2] );
                
                builder.AddParameter(
                    (string)parameters[i],
                    (string)parameters[i+1],
                    context );
            }
	        
			return builder.CreateDatatype();
		}

	}


	/// <summary>
	/// DatatypeLibraryFactory that can only load built-in datatypes. 
	/// </summary>
	public class DefaultDatatypeLibraryLoader : DatatypeLibraryFactory {
		
		public DatatypeLibrary CreateDatatypeLibrary(string namespaceURI) {
			if( namespaceURI.Equals("") )
				return BuiltinDatatypeLibrary.theInstance;
			return null;
		}
	}
}// end namespace

